(ns elasticsearch-geocoding.core
  (:gen-class)
  (:require [elasticsearch-geocoding.util :as util]
            [elasticsearch-geocoding.sparql :as sparql]
            [elasticsearch-geocoding.query :as query]
            [elasticsearch-geocoding.rdf :as rdf]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io :refer [as-file]]
            [schema.core :as s]
            [clojure.edn :as edn]
            [mount.core :as mount])
  (:import (org.apache.commons.validator.routines UrlValidator)
           (java.io IOException)))

; ----- Schemata -----

(def ^:private positive-integer (s/constrained s/Int pos? 'pos?))

(def ^:private non-negative-integer (s/constrained s/Int (complement neg?) 'not-neg?))

(def ^:private http? (partial re-matches #"^https?:\/\/.*$"))

(def ^:private valid-url?
  "Test if `url` is valid."
  (let [validator (UrlValidator. UrlValidator/ALLOW_LOCAL_URLS)]
    (fn [url]
      (.isValid validator url))))

(def ^:private url
  (s/pred valid-url? 'valid-url?))

(def ^:private Config
  {:sparql {:endpoint (s/conditional http? url)
            (s/optional-key :page-size) positive-integer
            (s/optional-key :start-from) non-negative-integer}
   :elasticsearch {:endpoint (s/conditional http? url)
                   :index s/Str
                   :mapping-type s/Str}})

; ----- Private functions -----

(defn- error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (util/join-lines errors)))

(defn- exit
  "Exit with @status and message `msg`.
  `status` 0 is OK, `status` 1 indicates error."
  [^Integer status
   ^String msg]
  {:pre [(#{0 1} status)]}
  (println msg)
  (System/exit status))

(def ^:private die
  (partial exit 1))

(def ^:private info
  (partial exit 0))

(defn- file-exists?
  "Test if file at `path` exists and is a file."
  [path]
  (let [file (as-file path)]
    (and (.exists file) (.isFile file))))

(defn- usage
  [summary]
  (util/join-lines ["Geocodes postal addresses using an Elasticsearch index."
                    "Outputs geo-coordinates in N-Triples."
                    "Options:\n"
                    summary]))

(defn- schema-validator
  [schema schema-name instance]
  (let [expected-structure (s/explain schema)]
    (try (s/validate schema instance) true
         (catch RuntimeException e (util/join-lines [(format "Invalid %s:" schema-name)
                                                     (.getMessage e)
                                                     (format "The expected structure of the %s is:" schema-name)
                                                     expected-structure])))))

(def ^:private validate-config
  "Validate configuration `config` according to its schema."
  (partial schema-validator Config "configuration"))

(defn- parse-config
  "Parse EDN configuration from `path`."
  [path]
  {:pre [(file-exists? path)]}
  (edn/read-string (slurp path)))

(defn- read-sparql
  "Read Mustache template with a SPARQL query from `path`."
  [path]
  {:pre [(file-exists? path)]}
  (slurp path))

(defn- main
  [config {:keys [output sparql]}]
  (mount/start-with-args config)
  (with-open [writer (io/writer output)]
    (doseq [geo-coordinates (->> sparql
                                 sparql/select-query-unlimited
                                 (pmap query/elasticsearch-geocode)
                                 (remove nil?)
                                 (map rdf/jsonld->ntriples))]
      (.write writer geo-coordinates))))

; ----- Private vars -----

(def ^:private cli-options
  [["-c" "--config CONFIG" "Path to configuration file in EDN"
    :parse-fn parse-config
    :validate [validate-config "Invalid configuration"]]
   ["-s" "--sparql SPARQL" "Path to SPARQL file to select resources"
    :parse-fn read-sparql]
   ["-o" "--output OUTPUT" "Path to the output file (default: STDOUT)"
    :default *out*]
   ["-h" "--help" "Display help message"]])

; ----- Public functions -----

(defn -main
  [& args]
  (let [{{:keys [config help]
          :as options} :options
         :keys [errors summary]} (parse-opts args cli-options)
        ; Merge defaults
        config' (util/deep-merge {:sparql {:page-size 5000
                                           :start-from 0}}
                                 config)]
    (cond help (info (usage summary))
          errors (die (error-msg errors))
          :else (main config' options))))
