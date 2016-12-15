(ns elasticsearch-geocoding.sparql
  (:require [elasticsearch-geocoding.endpoint :refer [endpoint]]
            [elasticsearch-geocoding.util :as util]
            [clj-http.client :as client]
            [stencil.core :refer [render-string]]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]))

; ----- Private functions -----

(def ^:private variable-binding-pair
  "Returns a pair of variable name and its binding."
  (juxt (comp keyword (zip-xml/attr :name)) zip-xml/text))

(defn- execute-query
  "Execute SPARQL query from `sparql-string`."
  [sparql-string & {:keys [accept attempts]
                    :or {attempts 0}}]
  (let [params (cond-> {:query-params {"query" sparql-string}
                        :throw-entire-message? true}
                 accept (assoc :accept accept))]
    (try+ (:body (client/get (:endpoint endpoint) params))
          (catch [:status 500] _
            (if (= attempts 5)
              (throw+)
              (do (Thread/sleep (+ (* attempts 2000) 1000))
                  (execute-query sparql-string :accept accept :attempts (inc attempts))))))))

; ----- Public functions -----

(defn construct-query
  "Execute SPARQL CONSTRUCT `sparql-string`."
  [sparql-string & {:keys [accept]
                    :or {accept "text/ntriples"}}]
  (execute-query sparql-string :accept accept))

(defn select-query
  "Execute SPARQL SELECT query in `sparql-string`
  Returns an empty sequence when the query has no results."
  [sparql-string]
  (doall (for [result (-> sparql-string
                          execute-query
                          xml/parse-str
                          :content
                          second
                          :content)
               :let [zipper (zip/xml-zip result)]]
           (->> (zip-xml/xml-> zipper :binding variable-binding-pair)
                (partition 2)
                (map vec)
                (into {})))))

(defn select-query-unlimited
  "Execute a SPARQL query rendered from `template` using `data`
  repeatedly with paging until empty results are returned."
  [template]
  (let [{:keys [page-size start-from]} endpoint
        paged-select (fn [offset]
                       (select-query (render-string template {:limit page-size :offset offset})))]
    (->> (iterate (partial + page-size) start-from)
         (map paged-select)
         (take-while seq)
         util/lazy-cat')))
