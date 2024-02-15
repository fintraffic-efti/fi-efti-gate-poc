(ns fintraffic.efti.db.flywaydb
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [fintraffic.io :as fio])
  (:import (java.io File)
           (java.util Map)
           (org.flywaydb.core Flyway)
           (org.flywaydb.core.api.configuration FluentConfiguration)
           (org.flywaydb.core.internal.configuration ConfigUtils)))

(def guide
  (str/join
    \newline
    ["Usage: clojure -M -m fintraffic.efti.db.flywaydb [command]"
     (str "Parameters: EFTI_DB_HOST, EFTI_DB_PORT, EFTI_DB_DATABASE_NAME, EFTI_DB_USER, EFTI_DB_PASSWORD "
          "can be defined as environment variables. ")
     "Supported commands are: "
     "- clean    - initialize a fresh database"
     "- migrate  - migrate to the latest database layout"
     "- repair   - attempt to fix migration checksum mismatches"]))

(def flyway-configuration
  {ConfigUtils/SCHEMAS                         "efti,audit"
   ConfigUtils/SQL_MIGRATION_PREFIX            "v"
   ConfigUtils/SQL_MIGRATION_SEPARATOR         "-"
   ConfigUtils/REPEATABLE_SQL_MIGRATION_PREFIX "r"
   ConfigUtils/LOCATIONS                       "classpath:migration"})

(defn file-placeholder-name [root file]
  (->> file (fio/relative-path root)
       (fio/path->str "/") (str "file:")))

(defn file-placeholders [^File root]
  (->> root file-seq
       (filter fio/file?)
       (map #(vector (file-placeholder-name root %)
                     (-> % slurp str/trim)))
       (into {})))
(defn env
  ([name default]
   (or (System/getenv (str "EFTI_DB_" (str/upper-case name))) default))
  ([name]
   (if-let [value (env name nil)] value
     (throw (IllegalArgumentException. (str "Missing environment variable: " name))))))

(defn flyway-placeholder-map [db]
  (assoc
    (->>
      "placeholder"
      (.getResources (fio/class-loader))
      enumeration-seq
      (map io/as-file)
      (filter fio/directory?)
      (map file-placeholders)
      (apply merge))
    "gateway-password" (:gateway-password db)))

(defn configure-flyway [db]
  (-> ^FluentConfiguration (Flyway/configure)
      (.dataSource (:url db) (:user db) (:password db))
      (.placeholders (flyway-placeholder-map db))
      (.cleanDisabled false)
      (.configuration ^Map flyway-configuration)
      .load))

(defn read-configuration []
  {:user (env "user" "efti")
   :password (env "password")
   :gateway-password (env "gateway_password")
   :url (str "jdbc:postgresql://" (env "host") ":"
             (env "port", 5432) "/" (env "database_name"))})

(defn run [args]
  (let [command (str/trim (or (first args) "<empty string>"))
        db (read-configuration)
        flyway (configure-flyway db)]
    (case command
      "clean" (.clean flyway)
      "migrate" (.migrate flyway)
      "repair" (.repair flyway)
      (do
        (println "Unsupported command: " command)
        (println guide)))))

(defn -main [& args] (run args))
