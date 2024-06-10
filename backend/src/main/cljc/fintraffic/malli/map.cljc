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

(defn rename-entry [f entry] (update entry 0 f))

(defn rename-entries [f malli]
  (malli/walk
    malli
    (fn [schema _path children options]
      (let [parent (malli/parent schema)
            properties (malli/properties schema)
            options (or (malli/options schema) options)]
        (if (map? schema)
          (malli/into-schema parent properties
                             (map (partial rename-entry f) children)
                             options)
          (malli/into-schema parent properties children options))))))