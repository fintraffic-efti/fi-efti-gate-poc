(ns fintraffic.efti.backend.muuntaja
  (:require [muuntaja.core :as muuntaja]
            [cognitect.transit :as transit]
            [clojure.string :as str])
  (:import [java.time Instant LocalDate]))

(def instant-transit-writer
  (transit/write-handler
    (constantly "t")
    (fn [^Instant instant] (.toString instant))))

(def local-date-transit-writer
  (transit/write-handler
    (constantly "t")
    (fn [^LocalDate instant] (.toString instant))))

(def time-transit-reader
  (transit/read-handler
    (fn [^CharSequence txt]
      (if (str/includes? txt "T")
        (Instant/parse txt)
        (LocalDate/parse txt)))))

(def instant-transit-options
  {:decoder-opts {:handlers {"t" time-transit-reader}}
   :encoder-opts {:handlers {Instant instant-transit-writer
                             LocalDate local-date-transit-writer}}})

(def instance
  (muuntaja/create
    (-> muuntaja/default-options
        (update-in
          [:formats "application/transit+json"]
          merge instant-transit-options)
        (update-in
          [:formats "application/transit+msgpack"]
          merge instant-transit-options))))
