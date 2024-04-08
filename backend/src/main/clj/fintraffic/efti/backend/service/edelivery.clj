(ns fintraffic.efti.backend.service.edelivery
  (:require [fintraffic.common.logic :as logic]
            [malli.transform :as malli-transform]
            [tick.core :as tick])
  (:import (java.time ZoneId)))

(def instant-format
  (-> tick/predefined-formatters
      :iso-local-date-time
      (.withZone (ZoneId/of "UTC"))))

(defn parse-instant [txt]
  (-> txt
      (tick/parse-zoned-date-time instant-format)
      tick/instant))

(def transformer
  (malli-transform/transformer
    (malli-transform/default-value-transformer
      ;; Add missing maybe-keys with nil values
      {:defaults {:maybe (constantly nil)}})
    {:name :edelivery
     :decoders
     (assoc (malli-transform/-string-decoders)
       'inst? (logic/when* string? parse-instant))
     :encoders
     (malli-transform/-string-encoders)}))