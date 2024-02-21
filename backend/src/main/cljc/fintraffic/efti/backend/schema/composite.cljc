(ns fintraffic.efti.backend.schema.composite
  "Tools to transform map values e.g. nil <-> {:a nil :b nil}
   This is needed when we have a schema like:
   :onset-age (lmalli/maybe {:value number? :unit string?})
   The API will not accept
   :onset-age {:value nil :unit nil}
   The API will accept
   :onset-age nil
   DB layer only understands
   :onset-age {:value nil :unit nil}
   This is needed for (flathead.flatten/tree->flat \"$\") to work."
  (:require [fintraffic.common.map :as map]
            [fintraffic.malli.map :as malli-map]
            [malli.core :as malli]
            [fintraffic.efti.backend.schema.common :as schema]
            [malli.transform :as malli-transform]))

(defn discard-incomplete
  "Replace composite with nil if it has a nil value and all the composite schema values are required"
  [composites entity]
  (reduce
    (fn [entity {:keys [path schema]}]
      (if (and (some->> (get-in entity path)
                        (merge (map/nil-values schema))
                        vals
                        (some nil?))
               ;; This doesn't handle a situation with a composite that has a nil value in a required field
               (->> schema
                    schema/schema
                    malli/children
                    (map #(nth % 2))
                    (some #(= :maybe (malli/type %)))
                    not))
        (assoc-in entity path nil)
        entity))
    entity
    composites))

(defn composite->db
  "For given path and schema, replace nil with a map with nil values.
   For example:
   (composite->db
     [:patient :onset-age]            ; path
     {:value number? :unit string?}  ; schema
     {:patient {:onset-age nil}})     ; entity
   => {:patient {:onset-age {:value nil, :unit nil}}}"
  [path schema entity]
  (if (map/contains-in? entity path)
    (update-in entity path #(if (nil? %)
                              (map/nil-values schema)
                              %))
    entity))

(defn ->nil [object] (if (->> object vals (every? nil?)) nil object))

(defn db->composite
  "For a given path, replace a map with only nil values with nil.
   For example:
   (db->composite
     [:patient :onset-age]                            ; path
     {:patient {:onset-age {:value nil, :unit nil}}}) ; entity
   => {:patient {:onset-age nil}}"
  [path entity] (update-in entity path ->nil))

(defn composites->db [composites entity]
  (reduce
    (fn [entity {:keys [path schema]}]
      (composite->db path schema entity))
    entity
    composites))

(defn db->composites [composites entity]
  (reduce
    (fn [entity {:keys [path]}]
      (db->composite path entity))
    entity
    composites))

(def discard-empty db->composites)

(defn- malli->clj [v]
  (if (satisfies? malli/Schema v)
    (malli/-form v)
    v))

(defn- malli-map [v]
  (and (vector? v)
       (= :map (first v))
       (into {} (rest v))))

(defn- malli-maybe-map [v]
  (let [v (malli->clj v)]
    (and (vector? v)
         (= :maybe (first v))
         (malli-map (second v)))))

(defn find-composite-schemas
  "Find composite schemas recursively.
   The result is meant to be used as input for composite->db and db->composite.
   For example:
   (find-composite-schemas fintraffic.efti.backend.schema.report.hum/Report)
   =>
   [{:path [:patient :gestation-period], :schema {:value number?, :unit int?}}
    {:path [:patient :onset-age], :schema {:value number?, :unit int?}}]"
  ([schema-map]
   (find-composite-schemas schema-map []))
  ([schema-map path]
   (reduce-kv
     (fn [result k v]
       (cond
         (map? v)
         (into result (find-composite-schemas v (conj path k)))

         (malli-maybe-map v)
         (conj result {:path (conj path k) :schema (malli-maybe-map v)})

         :else
         result))
     []
     schema-map)))

(defn map-schema->lite [schema]
  (->> schema
       malli/children
       (map (fn [[key _ schema]] [key schema]))
       (into {})))
(defn find-composite-children [schema]
  (->> schema
       malli/children
       (filter #(-> % (nth 2) malli/type (= :maybe)))
       (filter #(-> % (nth 2) malli/children first malli-map/map?))
       (map (fn [[key _ schema]]
              {:path [key]
               :schema (->> schema malli/children first
                            map-schema->lite)}))))

(defn compile-transform [accept convert schema _]
  (when (accept schema)
    (when-let [composites (-> schema find-composite-children seq)]
      #(convert composites %))))

(defn db-transformer
  ([] (db-transformer nil))
  ([{:keys [accept] :or {accept malli-map/map?}}]
   (malli-transform/transformer
     {:decoders {:map {:compile (partial compile-transform accept db->composites)}}
      :encoders {:map {:compile (partial compile-transform accept composites->db)}}})))

(defn discard-empty-decoder [schema]
  (malli/decoder
    (schema/schema schema)
    (malli-transform/transformer (db-transformer))))