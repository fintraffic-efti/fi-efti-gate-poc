(ns fintraffic.efti.backend.api.stream
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [fintraffic.efti.backend.exception :as exception]
            [org.httpkit.server :as http-kit]
            [ring.util.io :as ring-io])
  (:import (org.httpkit.server Channel)
           (java.io Writer)))

(defn- send! [^Channel channel headers body]
  (when-not (http-kit/send! channel {:headers headers :body body} false)
    (exception/throw-ex-info! :channel-closed "Response async channel is closed.")))

(defn result->async-channel [request response-headers result]
  (http-kit/as-channel
    request
    {:on-close (fn [_channel _status])
     :on-open
     (fn [channel]
       (future
         (try
           (result (partial send! channel response-headers))
           (catch Throwable t
             (if (some-> t ex-data :type (= :channel-closed))
               (log/info "Async channel closed in service: "
                          (exception/service-name request))
               (do
                 (log/error t "Sending response to async channel failed in service: "
                            (exception/service-name request)
                            (or (ex-data t) ""))
                 (send! channel response-headers
                        (str "ERROR: " (exception/exception-type t))))))
           (finally
             (http-kit/close channel)))))}))

(defn write! [^Writer writer ^String txt]
  (.write writer txt))

(defn result->piped-input-stream [request response-headers result]
  {:status 200
   :headers response-headers
   :body
   (ring-io/piped-input-stream
     (fn [out]
       (let [^Writer writer (io/writer out)]
         (try
           (result (partial write! writer))
           (catch Throwable t
             (log/error t "Sending response to stream failed in service: "
                        (exception/service-name request)
                        (or (ex-data t) "")))))))})
