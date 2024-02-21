(ns fintraffic.malli.map
  (:refer-clojure :exclude [map?])
  (:require [malli.core :as malli]
            [malli.util :as malli-util]))

(defn map? [schema]
  (= :map (malli/type schema)))

(defn dissoc-in [schema ks]
  (if (> (count ks) 1)
    (if (map? (malli-util/get-in schema (butlast ks)))
      (malli-util/update-in schema (butlast ks) malli-util/dissoc (last ks))
      schema)
    (malli-util/dissoc schema (first ks))))