(ns fintraffic.efti.schema.consignment
  (:require
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.schema.geo :as geo]
    [fintraffic.efti.schema.query :as query-schema]))

(def UIL
  {:gate-id     (schema/LimitedString 200)
   :platform-id (schema/LimitedString 200)
   :dataset-id  (schema/LimitedString 200)})

(def TransportEvent
  (schema/maybe-values
    {:actual-occurrence-date-time inst?}))

(def Country
  (schema/maybe-values
    {:id geo/CountryCode}))

(def TransportMeans
  (->
    {:identifier (schema/LimitedString 17)} ;eFTI618
    schema/maybe-values
    (assoc :registration-country Country))) ;eFTI620

(def TransportMovement
  (->
    {:dangerous-goods-indicator boolean?
     :transport-mode-code       (schema/ForeignKey :transport-mode)} ;eFTI581
    schema/maybe-values
    (assoc :used-transport-means TransportMeans)))

(def CarriedTransportEquipment
  (schema/maybe-values
    {:identifier           (schema/LimitedString 17) ;eFTI448
     :sequence-numeric     int?})) ;eFTI1000

(def TransportEquipment
  (->
    {:category-code (schema/LimitedString 3) ;eFTI378
     :identifier (schema/LimitedString 17) ;eFTI374
     :sequence-numeric int?} ;eFTI987
    schema/maybe-values
    (assoc
      :registration-country Country ;eFTI578
      :carried-transport-equipments (schema/vector CarriedTransportEquipment))))

(def ConsignmentSave
  (->
    {:carrier-acceptance-date-time inst?} ;eFTI39
    schema/maybe-values
    (assoc
      :delivery-event TransportEvent ;eFTI188
      :main-carriage-transport-movements (schema/vector TransportMovement)
      :utilized-transport-equipments (schema/vector TransportEquipment))))

(def Consignment (assoc ConsignmentSave :id schema/Id :uil UIL))

(def ConsignmentQuery
  (-> {:identifier (schema/LimitedString 200)
       :gate-ids (schema/vector (schema/LimitedString 200))}
      (merge (query-schema/Window 100))
      schema/optional-keys))

(def UILQuery (assoc UIL :subset-id keyword?))