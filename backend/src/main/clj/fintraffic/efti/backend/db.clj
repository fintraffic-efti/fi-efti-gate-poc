(ns fintraffic.efti.backend.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as jdbc-rs]
            [next.jdbc.protocols :as jdbc-protocols]
            [next.jdbc.transaction :as jdbc-transaction]
            [next.jdbc.prepare :as jdbc-prepare]
            [hikari-cp.core :as hikari]
            [hugsql.core :as hugsql]
            [hugsql.adapter :as hugsql-adapter]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as str]
            [clojure.set :as set]
            [next.jdbc.date-time]
            [clojure.tools.logging :as log]
            [jsonista.core :as json])
  (:import (org.postgresql.util PSQLException ServerErrorMessage PGobject)
           (java.time Instant)
           (java.sql Timestamp PreparedStatement Date Connection)
           (clojure.lang IPersistentVector Associative)
           (org.postgresql.jdbc PgArray)
           (java.util TimeZone)
           (org.postgresql.core Utils)))

(def ^:private fixed-db-options
  {:adapter        "postgresql"
   :current-schema "efti"
   ;; https://github.com/seancorfield/next-jdbc/blob/develop/doc/tips-and-tricks.md#batch-statements-2
   ;; Even when using next.jdbc/execute-batch!, PostgreSQL will still send multiple statements to the database unless
   ;; you specify :reWriteBatchedInserts true as part of the db-spec hash map or JDBC URL when the datasource is created.
   :reWriteBatchedInserts true})

(defn init! [options]
  (TimeZone/setDefault (TimeZone/getTimeZone "UTC"))
  ;; log db host for aws db config sanity check
  (log/info "db init: connecting to" (:host options))
  {:datasource
   (hikari/make-datasource
     (merge (set/rename-keys options
                             {:host :server-name
                              :port :port-number})
            fixed-db-options))})

(defn halt! [datasource]
  (-> datasource :datasource hikari/close-datasource))

(defn constraint [^ServerErrorMessage error]
  (keyword (str/replace (.getConstraint error) "_" "-")))

(defn value [^ServerErrorMessage error]
  (->> error .getDetail
       (re-find #"(?<=\=\().*(?=\))")))

(defn translatePSQLException [^PSQLException psqle]
  (if-let [^ServerErrorMessage error (.getServerErrorMessage psqle)]
    (case (.getSQLState error)
      "23505"
      (ex-info
        (.getMessage psqle)
        {:type       :unique-violation
         :constraint (constraint error)
         :value      (value error)}
        psqle)
      "23503"
      (ex-info
        (.getMessage psqle)
        {:type       :foreign-key-violation
         :constraint (constraint error)}
        psqle)
      psqle) psqle))

(defn with-db-exception-translation [db-function & args]
  (try
    (apply db-function args)
    (catch PSQLException psqle
      (throw (translatePSQLException psqle)))
    (catch Exception e
      (throw
        (let [cause (.getCause e)]
          (if (instance? PSQLException cause)
            (translatePSQLException cause) e))))))

;; A next/jdbc adapter for hugsql
;; see hugsql.adapter.next-jdbc/HugsqlAdapterNextJdbc
(deftype HugsqlAdapterNextJdbc []
  hugsql-adapter/HugsqlAdapter
  (execute [this db sqlvec {:keys [command-options] :as options}]
    (jdbc/execute! db sqlvec
                   (if (some #(= % (:command options)) [:insert :i!])
                     (assoc command-options :return-keys true)
                     command-options)))

  (query [this db sqlvec options]
    (jdbc/execute! db sqlvec (:command-options options)))

  (result-one [this result options] (first result))
  (result-many [this result options] result)
  (result-affected [this result options] (:next.jdbc/update-count (first result)))
  (result-raw [this result options] result)
  (on-exception [this exception]
    (throw exception)))

(def default-opts jdbc/unqualified-snake-kebab-opts)
(def batch-opts (assoc default-opts :batch true))
(def batch-no-keys-opts (assoc batch-opts
                          :return-keys false
                          :return-generated-keys false))

(defn require-queries
  ([name] (require-queries 'fintraffic.efti.backend.db name))
  ([ns-name name]
   (let [db-namespace (symbol (str ns-name "." name))]
     (binding [*ns* (create-ns db-namespace)]
       (hugsql/def-db-fns (str (str/replace db-namespace "." "/") ".sql")
                          {:quoting :off
                           :adapter (->HugsqlAdapterNextJdbc)})
       (hugsql/def-sqlvec-fns (str (str/replace db-namespace "." "/") ".sql")
                              {:quoting :off
                               :adapter (->HugsqlAdapterNextJdbc)}))
     (alias (symbol (str name "-db")) db-namespace))))

;;
;; Protocol extensions for next jdbc
;;

(defn escape-string-literal [^String txt] (Utils/escapeLiteral nil txt true))

(def keyword-keys-mapper
  (json/object-mapper {:decode-key-fn csk/->kebab-case-keyword}))

(defn pgobject->map
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (json/read-value value keyword-keys-mapper)
      value)))

(extend-protocol jdbc-protocols/Connectable
  Associative
  (get-connection [this opts]
    (let [set-variable-sql
          (fn [[key value]]
            (str "set efti.client_"
                 (-> key name csk/->snake_case) " = '"
                 (-> value str escape-string-literal) "';"))
          sqls (map set-variable-sql (:client this))
          ^Connection connection
          (-> this :datasource (jdbc-protocols/get-connection opts))]
      (jdbc/execute! connection [(str/join "" sqls)])
      connection)))

(extend-protocol jdbc-protocols/Executable
  Associative
  (-execute [this sql-params opts]
    (jdbc/on-connection [c this]
      (with-db-exception-translation jdbc-protocols/-execute c sql-params (merge default-opts opts))))
  (-execute-one [this sql-params opts]
    (jdbc/on-connection [c this]
      (with-db-exception-translation jdbc-protocols/-execute-one c sql-params (merge default-opts opts))))
  (-execute-all [this sql-params opts]
    (jdbc/on-connection [c this]
      (with-db-exception-translation jdbc-protocols/-execute-all c sql-params (merge default-opts opts)))))

(extend-protocol jdbc-protocols/Transactable
  Associative
  (-transact [this body-fn opts]
    (jdbc/on-connection [c this]
      (jdbc-protocols/-transact c body-fn (merge default-opts opts)))))

(extend-protocol jdbc-protocols/Preparable
  Associative
  (prepare ^PreparedStatement [this sql-params opts]
    (jdbc/on-connection [c this]
      (jdbc-protocols/prepare c sql-params (merge default-opts opts)))))

(defn transact
  "Executable needs to be an Associative for default options to work.
  Therefore, the transaction connection is wrapped in hash map.

  Set the nested-tx flag to :ignore to ignore the effect of opening nested transactions.
  Transactions are ran in the scope of the first opened transaction."
  [transactable f opts]
  (binding [jdbc-transaction/*nested-tx* :ignore]
    (jdbc/transact transactable (comp f #(hash-map :connectable %)) opts)))

(defmacro with-transaction
  "Same as jdbc/with-transaction except uses the transact-function defined above."
  [[sym transactable opts] & body]
  (let [con (vary-meta sym assoc :tag 'java.sql.Connection)]
    `(transact ~transactable (^{:once true} fn* [~con] ~@body) ~(or opts {}))))

(extend-protocol jdbc-rs/ReadableColumn
  Date
  (read-column-by-label [x _] (.toLocalDate x))
  (read-column-by-index [x _ _] (.toLocalDate x))

  Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]     (.toInstant v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3] (.toInstant v))

  PgArray
  (read-column-by-label [x _] (-> x .getArray vec))
  (read-column-by-index [x _ _] (-> x .getArray vec))

  PGobject
  (read-column-by-label [^PGobject v _]
    (pgobject->map v))
  (read-column-by-index [^PGobject v _2 _3]
    (pgobject->map v)))

(extend-protocol jdbc-prepare/SettableParameter
  IPersistentVector
  (set-parameter [^IPersistentVector v ^PreparedStatement stmt ^long i]
    (let [conn (.getConnection stmt)
          meta (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta i)]
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt i (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt i v))))

  Instant
  (set-parameter [^Instant instant ^PreparedStatement stmt ^long i]
    (.setObject stmt i (Timestamp/from instant))))
