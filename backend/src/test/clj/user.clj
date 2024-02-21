(ns user
  (:require
    [clojure.test :as t]
    [clojure.tools.namespace.repl :as tools-repl]
    [eftest.runner :as eftest]
    [fintraffic.common.maybe :as maybe]
    [fintraffic.efti.backend.config :as config]
    [fintraffic.efti.backend.dev :as dev]
    [fintraffic.efti.backend.system :as system]
    [flathead.deep :as deep]))

(defonce backend (atom nil))
(defn stop! [] (swap! backend (maybe/lift1 system/halt!)))
(defn start! []
  (swap!
    backend
    #(do (maybe/map* system/halt! %)
         (system/start! (deep/deep-merge
                          dev/config
                          (config/from-environment (System/getenv)))))))

(defn db-client
  ([user-id] (-> @backend :db (db-client user-id)))
  ([db user-id]
   {:pre [(integer? user-id)]}
   (-> db
       (assoc-in [:client :id] user-id)
       (assoc-in [:client :service-uri] "backend.efti.test"))))

(system/add-shutdown-hook! stop!)

(defn db []
  (db-client -10))

(defn reload! []
  (stop!)
  (tools-repl/refresh-all {:after 'user/start!}))

; (run-test #'fintraffic.efti.schema.user-test/valid-ssn-fi?-test)
(defn run-test [var-name]
  (t/run-test-var var-name))

(defn run-eftest [source]
  (-> (eftest/find-tests source)
      (eftest/run-tests {:fail-fast? true})))

(defn run-tests []
  (try
    (-> (eftest/find-tests "src/test")
        (eftest/run-tests {:fail-fast? true}))
    (catch Exception e
      (println e))))

(defn run-tests+exit-error-code! []
  (let [{:keys [fail error]} (run-tests)]
    (System/exit (if (and (zero? fail) (zero? error)) 0 1))))
