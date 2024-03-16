(ns fintraffic.efti.backend.config
  (:require [clojure.string :as str]
            [cheshire.core :as cheshire]
            [flathead.flatten :as flat]
            [malli.core :as malli]
            [malli.util :as malli-util]
            [malli.error]
            [malli.transform :as malli-transform]
            [malli.experimental.lite :as lmalli]
            [fintraffic.common.map :as map]
            [clojure.set :as set]
            [fintraffic.efti.backend.exception :as exception]
            [clojure.tools.logging :as log]))

(def schema
  (lmalli/schema
    {:db
     {:host          string?
      :port          int?
      :username      string?
      :password      string?
      :database-name string?}

     :environment keyword?
     :gate-id     string?

     :web         {:csp boolean?}

     :nrepl       (lmalli/optional {:port int?})

     :http-server
     {:port     int?
      :max-body int?
      :thread   int?}}))

(def default-config
  {:db          {:port 5432}
   :environment :dev
   :web         {:csp true}
   :http-server
   {:max-body (* 1024 1024 50)
    :thread   20
    :port     8080}})

(defn path->env-key-name [path]
  (->> path
       (map (comp str/upper-case #(str/replace % "-" "_") name))
       (str/join "_")
       (str "EFTI_")))

(defn path->flat-config-key [path]
  (->> path
       (map name)
       (str/join ".")
       keyword))

(def environment-key-map
  (let [paths (->> schema malli-util/subschemas
                   (filter #(not= :map (-> % :schema malli/type)))
                   (map :in))
        env-keys (map path->env-key-name paths)
        flat-config-keys (map path->flat-config-key paths)]
    (into {} (map vector env-keys flat-config-keys))))

(def ^:private string-decoder
  (malli/decoder schema malli-transform/string-transformer))

(defn aws-db-config-from-env
  "Copilot passes DB config in env var named by db, here MAINDB_SECRET."
  [env-map]
  (when-let [env-val (get env-map "MAINDB_SECRET")]
    (let [{:keys [host port dbname]}
            (cheshire/decode env-val keyword)]
      {:db {:host          host :port port
            ;; :username is defined in merged-to config
            :database-name dbname}})))

(defn from-environment [env]
  (as-> env %
        (map/filter-keys #(str/starts-with? % "EFTI_") %)
        (set/rename-keys % environment-key-map)
        (flat/flat->tree #"\." %)
        (string-decoder %)
        (merge (aws-db-config-from-env env) %)))

(def ^:private explainer (malli/explainer schema))
(defn validate! [config]
  (when-let [errors (explainer config)]
    (log/error "Validation error in config" (malli.error/humanize errors))
    (exception/throw-ex-info!
      {:type :invalid-configuration
       :message "Invalid system configuration"
       :errors (malli.error/humanize errors)})))
