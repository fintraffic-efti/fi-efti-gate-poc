(ns fintraffic.efti.schema
  (:refer-clojure :exclude [vector])
  (:require
    [fintraffic.common.logic :as logic]
    [fintraffic.common.map :as map]
    [fintraffic.malli.time :as malli-time]
    [malli.core :as malli]
    [malli.experimental.lite :as lmalli]
    [malli.transform :as malli-transform]))

(def options {:registry (merge (malli/default-schemas) (malli-time/time-schemas))})

(defn schema [lite-malli]
  (binding [lmalli/*options* options]
    (lmalli/schema lite-malli)))

(defn maybe [lite-malli]
  (binding [lmalli/*options* options]
    (malli/form (lmalli/maybe lite-malli))))

(defn discard-invalid
  "Discards invalid values from entity. Returns entity with invalid values set to nil."
  [entity schema]
  (let [errors (:errors (malli/explain schema entity))]
    (reduce (fn [acc error]
              (assoc-in acc (:in error) nil))
            entity
            errors)))

(defn vector [lite-malli]
  (binding [lmalli/*options* options]
    (malli/form (lmalli/vector lite-malli))))

(def maybe? (every-pred sequential? #(= (first %) :maybe)))

(defn maybe-values [lite-malli]
  (map/map-values (logic/unless* maybe? maybe) lite-malli))

(defn strip-maybe [val]
  (if (maybe? val)
    (second val)
    val))

(defn required-values [lite-malli keys]
  (as-> lite-malli $
       (select-keys $ keys)
       (map/map-values strip-maybe $)
       (merge lite-malli $)))

(defn optional-keys [lite-malli]
  (map/map-values lmalli/optional lite-malli))

(defn strip-extra-keys [malli-schema value]
  (malli/decode
    (schema malli-schema)
    value
    malli-transform/strip-extra-keys-transformer))

;; Represents a primary/foreign key
(def Key [:int {:fintraffic.efti.schema/type :key}])

;; Represents an ext_id column (unique constraint)
(def ExtID [:int {:fintraffic.efti.schema/type :ext-id}])

(defn ForeignKey
  "Represents a foreign key. Table name can be used by e2e tests in select/classification."
  [table]
  [:int {:fintraffic.efti.schema/type :foreign-key
         :table table}])

(def Id {:id Key})

(defn Limited
  [type type-name type-key min max]
  [type {:title                          (str type-name "[" min ", " max "]")
         :fintraffic.efti.schema/type type-key
         :min                            min, :max max}])

(defn LimitedString
  ([max] (LimitedString 1 max))
  ([min max]
   (Limited
     :string
     "String"
     (if (= min max)
       :limited.string-exact
       :limited.string)
     min
     max)))

(defn valid-email?
  "Email address validation
  Email address supports unicode characters:
  https://stackoverflow.com/questions/3844431/are-email-addresses-allowed-to-contain-non-alphanumeric-characters
  https://en.wikipedia.org/wiki/Email_address"
  [string]
  (re-matches #"(?u)[\p{L}0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[\p{L}0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
              string))

(defn add-validation
  ([title schema-type msg valid? schema]
   [:and schema [:fn
                 {:title                          title
                  :fintraffic.efti.schema/type schema-type
                  :error/message                  msg} valid?]])
  ([schema-type valid? schema]
   (add-validation (str "Valid " (-> schema-type name)) schema-type
                   (str "Invalid " (-> schema-type name))
                   valid? schema)))
(def Email (add-validation :email valid-email? (LimitedString 100)))

(defn LimitedInt [min-inclusive max-inclusive]
  (Limited :int "Int" :limited.int min-inclusive max-inclusive))

(def Classification
  (merge Id {:label-fi [:maybe string?]
             :label-sv [:maybe string?]
             :label-en string?
             :valid    boolean?}))

(def ClassificationEN
  (dissoc Classification :label-fi :label-sv))

(def ConstraintError
  {:type       keyword?
   :constraint keyword?})

(def CommonError
  {:type    keyword?
   :message string?})

(defn QueryWindow [max-limit]
  {:limit  (LimitedInt 1 max-limit)
   :offset int?})

(def ResultCount
  {:count int?})
