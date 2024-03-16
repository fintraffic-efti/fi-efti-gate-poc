(ns fintraffic.efti.schema.consignment
  (:require
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.schema.geo :as geo]))

(def UIL
  {:gate-id     (schema/LimitedString 200)
   :platform-id (schema/LimitedString 200)
   :data-id     (schema/LimitedString 200)})

(def TransportVehicle
  (schema/maybe-values
    {:transport-mode-id  (schema/ForeignKey :transport-mode)
     :vehicle-id         (schema/LimitedString 200)
     :vehicle-country-id geo/CountryCode
     :journey-start-time inst?
     :journey-end-time   inst?
     :country-start-id   geo/CountryCode
     :country-end-id     geo/CountryCode}))

(def ConsignmentSave
  (->
    {:is-dangerous-goods boolean?
     :journey-start-time inst?
     :journey-end-time   inst?
     :country-start-id   geo/CountryCode
     :country-end-id     geo/CountryCode}
    schema/maybe-values
    (assoc :transport-vehicles (schema/vector TransportVehicle))))

(def Consignment (assoc (merge ConsignmentSave UIL) :id schema/Id))

(def ConsignmentQuery
  (schema/optional-keys
    {:vehicle-id (schema/LimitedString 200)}))