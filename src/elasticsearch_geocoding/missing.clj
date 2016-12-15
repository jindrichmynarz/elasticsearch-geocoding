(ns elasticsearch-geocoding.missing
  (:gen-class)
  (:require [elasticsearch-geocoding.sparql :as sparql]
            [elasticsearch-geocoding.query :refer [geocode sparql-geocode sparql-geocode-query]]
            [elasticsearch-geocoding.elasticsearch :refer [elasticsearch]]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [mount.core :as mount]
            [slingshot.slingshot :refer [try+ throw+]]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [cheshire.core :as json]))

(defn present?
  "Is `id` present?"
  [id]
  (let [{:keys [conn index mapping-type]} elasticsearch]
    (esd/present? conn index mapping-type id)))

(defn batch-missing?
  [ids]
  (let [{:keys [endpoint index mapping-type]} elasticsearch
        body (json/generate-string {:size 0
                                    :query {:ids {:type mapping-type
                                                  :values ids}}})
        url (str endpoint "/" index "/" mapping-type "/_search")
        count-missing (partial - (count ids))
        missing? (-> url
                     (client/post {:body body :as :json})
                     (get-in [:body :hits :total])
                     count-missing
                     pos?)]
    (when missing?
      (remove present? ids))))

(defn -main
  [& _]
  (mount/start-with-args (edn/read-string (slurp "config.edn")))
  (let [iris (->> "select_query.mustache"
                  io/resource
                  slurp
                  sparql/select-query-unlimited
                  (map :resource))
        missing-ids (->> iris
                         (partition-all 1000)
                         (mapcat batch-missing?))]
    (doseq [missing-id missing-ids]
      (println missing-id))))
