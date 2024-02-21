(ns fintraffic.efti.backend.service.concurrent
  (:require [clojure.tools.logging :as log]
            [fintraffic.common.map :as map]))

(defn safe
  "Catch all exception and log exceptions with given error description"
  [fn error-description]
  #(try
    (fn)
    (catch Throwable t
      (log/error t error-description (or (ex-data t) ""))
      nil)))

(defn run-background
  "Executes the given function asynchronously as a background service.
  Returns immediately nil and exceptions are only logged.
  Execution is implemented using clojure future."
  [fn error-description]
  (future-call (safe fn error-description))
  nil)

(defn error? [error-description]
  #(if-let [error (ex-data %)]
     (map/submap? error-description error)
     false))

(defn retry [fn amount wait retry?]
  #(loop [counter amount]
    (when (> counter 0)
      (let [value (try (apply fn %&) (catch Throwable t t))]
        (if (retry? value)
          (do
            (when (> wait 0) (Thread/sleep wait))
            (recur (dec counter)))
          (if (instance? Throwable value)
            (throw value)
            value))))))

(defn call! "Execute a function synchronously for its side effects"
  [fn] (fn))
