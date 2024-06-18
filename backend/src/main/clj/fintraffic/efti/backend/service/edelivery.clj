(ns fintraffic.efti.backend.service.edelivery
  (:require [camel-snake-kebab.core :as csk]
            [clojure.walk :as walk]
            [fintraffic.common.collection :as collection]
            [fintraffic.common.logic :as logic]
            [fintraffic.common.xml :as fxml]
            [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as db-query]
            [fintraffic.efti.schema :as schema]
            [fintraffic.efti.schema.consignment :as consignment-schema]
            [fintraffic.efti.schema.edelivery :as edelivery-schema]
            [fintraffic.efti.schema.edelivery.message-direction :as message-direction]
            [fintraffic.efti.schema.edelivery.message-type :as message-type]
            [flathead.plain :as plain]
            [malli.core :as malli]
            [malli.transform :as malli-transform]
            [next.jdbc.sql :as sql]
            [tick.core :as tick])
  (:import (java.time ZoneId)
           (java.time.format DateTimeFormatterBuilder)
           (java.time.temporal ChronoField)))

(db/require-queries 'edelivery)

(def instant-format
  (-> (DateTimeFormatterBuilder.)
      (.appendPattern "uuuu-MM-dd'T'HH:mm:ss")
      (.optionalStart)
      (.appendLiteral \.)
      (.appendFraction ChronoField/NANO_OF_SECOND, 1, 9, false)
      (.optionalEnd)
      (.appendPattern "[X]")
      (.parseDefaulting ChronoField/NANO_OF_SECOND, 0)
      .toFormatter
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

(defn add-message [db message]
  (sql/insert! db :ed-message message db/default-opts))

(defn find-messages [db conversation-id]
  (db-query/find-by db :ed-message edelivery-schema/Message
                    {:conversation-id (str conversation-id)
                     :type-id         message-type/response
                     :direction-id    message-direction/in}))

(defn find-messages-until [db conversation-id predicate timeout]
  (let [response (atom [])
        start-time (System/currentTimeMillis)]
    (while (and (not (predicate @response))
                (< (- (System/currentTimeMillis) start-time) timeout))
      (reset! response (find-messages db conversation-id))
      (Thread/sleep 1000))
    @response))


(defn new-conversation-id [db]
  (-> (edelivery-db/select-next-conversation-id-seq db) first :nextval))

(def lists
  {:consignments :consignment
   :mainCarriageTransportMovements :mainCarriageTransportMovement
   :utilizedTransportEquipments :utilizedTransportEquipment
   :carriedTransportEquipments :carriedTransportEquipment})

(def order
  [:uil :carrierAcceptanceDateTime :deliveryTransportEvent :utilizedTransportEquipment :mainCarriageTransportMovement
   :gateId :platformId :dataId
   :categoryCode :identifier :registrationCountry :sequenceNumeric :carriedTransportEquipment
   :transportModeCode :dangerousGoodsIndicator :usedTransportMeans])

(defn rename-properties-object [rename object]
  (walk/postwalk (logic/when* map? #(plain/map-keys rename %)) object))

(defn object->xml [element-key value]
  (->> value
       (rename-properties-object csk/->camelCaseKeyword)
       (fxml/object->xml+nowrap lists element-key)
       (fxml/reorder-children (comp #(collection/find-index (partial = %) order) first))))

(defn xml->object [xml]
  (->> xml
       (fxml/xml->object+nowrap lists)
       (rename-properties-object csk/->kebab-case-keyword)))

(defn consignments->xml [consignments]
  (object->xml :consignments {:consignments consignments}))

(def coerce-consignment
  (malli/coercer (schema/schema consignment-schema/Consignment) transformer))

(defn xml->consignments [xml]
  (->> xml xml->object :consignments (map coerce-consignment)))

(defn uil->xml [uil] (object->xml :uil  uil))
(defn xml->uil [xml] (xml->object xml))

(defn query->xml [uil] (fxml/object->xml :query {} uil))
(def coerce-query
  (malli/coercer (schema/schema consignment-schema/ConsignmentQuery) transformer))
(defn xml->query [xml] (->> xml (fxml/xml->object #{}) coerce-query))