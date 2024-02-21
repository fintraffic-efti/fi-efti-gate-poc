(ns fintraffic.efti.backend.test-system
  (:require
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [user :as user]
    [fintraffic.efti.backend.db :as db]
    [fintraffic.efti.backend.dev :as dev]
    [fintraffic.efti.schema.role :as role]))

(def ^:dynamic *db* nil)
(def ^:dynamic *admin-db* nil)

(defn db-client
  ([] (db-client (:database role/system-users)))
  ([user-id]
   {:pre [(integer? user-id)]}
   (user/db-client *db* user-id)))

(defn create-db! [db db-name]
  (jdbc/execute! db
                 [(format "create database %s template efti_template" db-name)]
                 {:transaction? false}))

(defn drop-db! [db db-name]
  (jdbc/execute! db
                 [(format "drop database if exists %s" db-name)]
                 {:transaction? false}))

(defn db-uuid []
  (-> (random-uuid)
      .toString
      (str/replace "-" "")))

(def admin-db-config
  (assoc (:db dev/config)
    :username "efti"))

(defn test-db-config [database-name]
  (assoc (:db dev/config)
    :database-name database-name))

(defn fixture [f]
  (let [db-name (str "efti_test_" (db-uuid))
        admin-db (db/init! admin-db-config)]
    (try
      (create-db! admin-db db-name)
      (let [test-db (db/init! (test-db-config db-name))]
        (with-bindings
          {#'*db* test-db
           #'*admin-db* admin-db}
          (try (f) (finally (db/halt! test-db)))))
      (finally
        (drop-db! admin-db db-name)
        (db/halt! admin-db)))))
