(ns fintraffic.efti.backend.db.query
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [fintraffic.common.logic :as logic]
    [fintraffic.common.maybe :as maybe]
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.schema.composite :as composite]
    [fintraffic.malli.collection :as malli-collection]
    [flathead.flatten :as flat]
    [malli.transform :as malli-transform]
    [malli.util :as malli-util]
    [malli.core :as malli]
    [next.jdbc.sql :as jdbc-sql]
    [fintraffic.efti.backend.db :as db]
    [tick.core :as tick]))

(defn schema->columns
  "Returns db column keywords for given malli-schema."
  ([malli-schema] (schema->columns malli-schema {}))
  ([malli-schema {:keys [vectors?]
                  :or {vectors? false}}]
   (let [remove-types
         (if vectors? #{:maybe :map :fn}
                      #{:maybe :map :vector :fn})]
     (->> malli-schema
          schema/schema
          malli-util/subschemas
          (remove (fn [{:keys [path schema]}]
                    (or (-> schema malli/type remove-types)
                        (-> path set :malli.core/in))))
          (mapv (fn [{:keys [path]}]
                  (->> path
                       (remove #(= 0 %))  ; zero in the path means :maybe
                       (map name)
                       (str/join "$")
                       keyword)))))))

(defn find-all
  ([db table schema] (find-all db table schema [[:ordinal :asc]]))
  ([db table schema order-by]
   (jdbc-sql/find-by-keys
     db table :all
     (assoc db/default-opts
       :columns (schema->columns schema)
       :order-by order-by))))

(defn find-by
  ([db table schema where]
   (jdbc-sql/find-by-keys
     db table where
     (assoc db/default-opts :columns (schema->columns schema))))
  ([db table schema where order-by]
   (jdbc-sql/find-by-keys
     db table where
     (assoc db/default-opts
       :columns (schema->columns schema)
       :order-by order-by))))

(defn flat->tree [m]
  (walk/postwalk
    (logic/when* map? #(flat/flat->tree #"\$" %))
    m))

(defn assoc-ignore-flag [option-key query]
  (let [ignore-key (->> option-key name
                        (str "ignore-") keyword)
        option-id-key (-> option-key name (str "-id") keyword)]
    (-> query
        (assoc ignore-key (-> query option-id-key nil?))
        (update option-id-key #(when (not= % :none) %)))))

(def string-transformer
  (malli-transform/transformer
    {:name :string
     :decoders (assoc (malli-transform/-string-decoders)
                 'number? (maybe/lift1 bigdec)
                 'inst? (maybe/lift1 tick/instant))
     :encoders (malli-transform/-string-encoders)}))

(defn decoder [schema]
  (malli/decoder
    (schema/schema schema)
    (malli-transform/transformer
      (composite/db-transformer)
      string-transformer
      malli-collection/nil->empty-collection-transformer
      malli-transform/strip-extra-keys-transformer)))