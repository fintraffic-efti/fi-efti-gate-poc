(ns fintraffic.efti.backend.schema.geo
  (:require
    [fintraffic.efti.backend.schema.common :as schema]))

(def CountryCode
  [:string {:title "ISO 3166-1 alpha-2" :min 2, :max 2}])

(def LanguageCode
  [:string {:title "ISO 639-2/T alpha-3" :min 3, :max 3}])

(def CountryCodeA3
  [:string {:title "ISO 3166-1 alpha-3" :min 3, :max 3}])

(def Country
  (assoc schema/Classification :id CountryCode :alpha-3 CountryCodeA3))

(def Language
  (assoc schema/ClassificationEN :id LanguageCode))

(def eu "EU")

(defn eu?
  "Returns true for country code 'EU'.
  R3 / G.k.3.2:
  The country code  EU exists in the ISO 3166 country code list as an exceptional reservation code to support any
  application that needs to represent the name European Union. In this case, ‘EU’ will be accepted as the country code."
  [{:keys [id]}]
  (= eu id))

;; Returns true if option is a valid country (not EU)
(def valid-country?
  (every-pred :valid (complement eu?)))
