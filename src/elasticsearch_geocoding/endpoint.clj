(ns elasticsearch-geocoding.endpoint
  (:require [mount.core :as mount :refer [defstate]]
            [clj-http.client :as client]))

(defn init-endpoint
  "Ping endpoint to test if it is up."
  [{:keys [sparql]}]
  (client/head (:endpoint sparql) {:throw-entire-message? true})
  sparql)

(defstate endpoint
  :start (init-endpoint (mount/args)))
