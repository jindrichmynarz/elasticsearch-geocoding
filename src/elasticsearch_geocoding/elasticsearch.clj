(ns elasticsearch-geocoding.elasticsearch
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [mount.core :as mount :refer [defstate]]))

(defn init
  [{{:keys [endpoint index mapping-type]
     :as es} :elasticsearch}]
  (let [conn (es/connect endpoint)]
    (when-not (esi/exists? conn index)
      (throw (ex-info "IndexMissing"
                      {:message (format "Index %s doesn't exist!" index)})))
    (when-not (esi/type-exists? conn index mapping-type)
      (throw (ex-info "MappingTypeMissing"
                      {:message (format "Mapping type %s doesn't exist!" mapping-type)})))
    (assoc es :conn conn)))

(defstate elasticsearch
  :start (init (mount/args))
  :stop (.close elasticsearch))
