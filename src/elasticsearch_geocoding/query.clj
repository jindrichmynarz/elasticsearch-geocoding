(ns elasticsearch-geocoding.query
  (:require [elasticsearch-geocoding.elasticsearch :refer [elasticsearch]]
            [elasticsearch-geocoding.rdf :refer [jsonld->ntriples]]
            [elasticsearch-geocoding.util :refer [->double]]
            [clojurewerkz.elastisch.rest.document :as document]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as response]
            [clojure.string :as string]))

; ----- Private functions -----

(defn- hit->jsonld
  [{{:keys [location]} :_source
    id :_id}]
  (let [[latitude longitude] (string/split location #",")]
    {"@context" {"@vocab" "http://schema.org/"
                 "xsd" "http://www.w3.org/2001/XMLSchema#"
                 "latitude" {"@type" "xsd:decimal"}
                 "longitude" {"@type" "xsd:decimal"}}
     "address" {"@id" id}
     "geo" {"latitude" latitude
            "longitude" longitude}}))

(defn- match
  [field query & {:as params}]
  {:match {field (merge {:query query} params)}})

(defn- multi-match
  [fields query & {:as params}]
  {:multi_match (merge {:fields fields
                        :query query}
                       params)})

; ----- Public functions -----

(defn elasticsearch-geocode-query
  "Prepare Elasticsearch geocoding query from a postal address."
  [{:keys [postalAddress streetAddress addressLocality postalCode
           houseNumber orientationalNumber orientationalNumberLetter]}]
  (let [must-match (cond-> []
                     streetAddress (conj (match :streetAddress streetAddress))
                     (and addressLocality
                          postalCode) (conj {:bool {:should [(match :addressLocality addressLocality
                                                                    :prefix_length 3)
                                                             (match :postalCode postalCode
                                                                    :boost 3)]}})
                     (and addressLocality
                          (not postalCode)) (conj (match :addressLocality addressLocality
                                                         :prefix_length 3))
                     (and postalCode
                          (not addressLocality)) (conj (match :postalCode postalCode
                                                              :boost 3))
                     houseNumber (conj (multi-match [:houseNumber :orientationalNumber] houseNumber)))
        should-match (cond-> []
                       orientationalNumber (conj (multi-match [:houseNumber :orientationalNumber] 
                                                              orientationalNumber))
                       orientationalNumberLetter (conj (match :orientationalNumberLetter 
                                                              orientationalNumberLetter)))
        pattern (cond-> {}
                  (seq must-match) (assoc :must must-match)
                  (seq should-match) (assoc :should should-match))]
    {:_source [:location]
     :query {:bool pattern}}))

(defn elasticsearch-geocode
  [postal-address]
  (when (seq (dissoc postal-address :postalAddress))
    (let [{:keys [conn index mapping-type]} elasticsearch
          query (elasticsearch-geocode-query postal-address)
          resp (document/search conn index mapping-type query)]
      (when (response/any-hits? resp)
        (-> resp
            response/hits-from
            first
            hit->jsonld)))))
