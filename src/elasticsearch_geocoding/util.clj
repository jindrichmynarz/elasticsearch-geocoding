(ns elasticsearch-geocoding.util
  (:require [clojure.string :as string]))

(defn ->double
  "Cast string `s` to double."
  [s]
  (Double/parseDouble s))

(defn deep-merge
  "Deep merge maps.
  Stolen from <https://github.com/clojure-cookbook/clojure-cookbook/blob/master/04_local-io/4-15_edn-config.asciidoc>."
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(def join-lines
  (partial string/join \newline))

(defn lazy-cat'
  "Lazily concatenates lazy sequence of sequences @colls.
  Taken from <http://stackoverflow.com/a/26595111/385505>."
  [colls]
  (lazy-seq
    (if (seq colls)
      (concat (first colls) (lazy-cat' (next colls))))))
