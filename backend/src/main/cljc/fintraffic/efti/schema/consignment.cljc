(ns fintraffic.efti.schema.consignment
  (:require
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.schema.geo :as geo]))

(def UIL
  {:platform-url (schema/LimitedString 200)
   :gate-url     (schema/LimitedString 200)
   :data-id      (schema/LimitedString 200)})

(def ConsignmentSave
  (schema/maybe-values
    {:is-dangerous-goods boolean?
     :journey-start-time inst?
     :journey-end-time   inst?
     :country-start-id   geo/CountryCode
     :country-end-id     geo/CountryCode}))

(def Consignment (assoc (merge ConsignmentSave UIL) :id schema/Id))