(ns fintraffic.efti.backend.http-server
  (:require [org.httpkit.server :as http-kit]
            [fintraffic.efti.backend.handler :as handler]
            [clojure.tools.logging :as log]))

(defn init! [options resources]
  (let [handler (handler/handler (:config resources))]
    (http-kit/run-server
      #(handler (merge % resources))
      (assoc options
        :error-logger (fn [txt ex] (log/error ex txt))
        :warn-logger (fn [txt ex] (log/warn ex txt))))))

(defn halt! [server] (server :timeout 100))
