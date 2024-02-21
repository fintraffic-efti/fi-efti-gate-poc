(ns fintraffic.efti.backend.system
  (:require [clojure.tools.logging :as log]
            [fintraffic.common.map :as map]
            [fintraffic.efti.backend.config :as config]
            [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.exception :as exception]
            [fintraffic.efti.backend.http-server :as http-server]
            [flathead.deep :as deep]
            [nrepl.server :as nrepl-server]))

(def db-init! (exception/safe db/init!))
(def nrepl-server-init! (exception/safe nrepl-server/start-server))
(def http-server-init! (exception/safe http-server/init!))

(defn init!
  "Initializes a new system based on the given configuration
  and starts all system components. Returns the initialized system."
  [config]
  (config/validate! config)
  (let [db (-> config :db db-init!)
        nrepl-server (some->> config :nrepl :port (nrepl-server-init! :port))
        http-server (-> config :http-server
                        (http-server-init! (map/bindings->map config db)))]
    (map/bindings->map config db http-server nrepl-server)))

(defn- halt-subsystem! [system subsystem-key halt-fn]
  (try
    (if-let [subsystem (subsystem-key system)]
      (exception/map halt-fn subsystem))
    (catch Throwable t
      (log/error t (str "Failed to halt subsystem: " (name subsystem-key))))))

(defn halt!
  "Halt system. Halts all subsystems even if halt fails for some subsystem.
  Do nothing if subsystem start is failed."
  [system]
  (halt-subsystem! system :http-server http-server/halt!)
  (halt-subsystem! system :nrepl-server nrepl-server/stop-server)
  (halt-subsystem! system :db db/halt!)
  nil)

(defn start!
  "Start a new system.
  If all components are not started successfully then halt system."
  [config]
  (let [system (init! config)
        errors (->> system vals (filter exception/throwable?))]
    (when-not (empty? errors)
      (halt! system)
      (throw (first errors)))
    system))


(defn add-shutdown-hook! [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. f)))

(defn environment-config []
  (deep/deep-merge
    config/default-config
    (config/from-environment (System/getenv))))

(defn -main []
  (try
    (let [system (start! (environment-config))]
      (add-shutdown-hook! #(halt! system)))
    (catch Throwable t
      (log/fatal t "System start failed -" (or (ex-data t) (ex-message t))))))
