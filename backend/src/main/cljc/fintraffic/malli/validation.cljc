(ns fintraffic.malli.validation
  (:require [malli.core :as malli]
            [malli.util :as malli-util]))

(defn maybe? [schema]
  (= (malli/type schema) :maybe))

(defn collection? [schema]
  (#{:set :vector :sequential} (malli/type schema)))


(defn required [schema schema-path]
  {:pre [(some-> schema (malli-util/get-in schema-path) maybe?)]}
  (malli-util/update-in schema schema-path #(-> % malli/children first)))

(defn assoc-properties [schema k value]
  (malli-util/update-properties schema #(assoc % k value)))

(defn collection-not-empty [schema schema-path]
  {:pre [(some-> schema (malli-util/get-in schema-path) collection?)]}
  (malli-util/update-in schema schema-path #(assoc-properties % :min 1)))

(defn replace-schema [schema old-schema new-schema]
  (malli/walk
    schema
    (malli/schema-walker
      #(if (malli-util/equals % old-schema) new-schema %))))


