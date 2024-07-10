(ns fintraffic.efti.backend.service.edelivery
  (:require [camel-snake-kebab.core :as csk]
            [clojure.walk :as walk]
            [clojure.data.xml :as xml]
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
            [fintraffic.efti.schema.subset :as subset]
            [flathead.plain :as plain]
            [malli.core :as malli]
            [malli.transform :as malli-transform]
            [next.jdbc.sql :as sql]
            [tick.core :as tick])
  (:import (java.time ZoneId)
           (java.time.format DateTimeFormatterBuilder)
           (java.time.temporal ChronoField)))

(db/require-queries 'edelivery)

(def namespaces
  {:efti-ed {:namespace "http://efti.eu/v1/edelivery"
             :tags #{:uilQuery :subsetId :uilResponse :identifierQuery :identifierResponse}}
   :efti-id {:namespace "http://efti.eu/v1/consignment/identifier"
             :tags #{:uil :gateId :platformId :datasetId}}
   :efti    {:namespace "http://efti.eu/v1/consignment"
             :tags #{:carrierAcceptanceDateTime :deliveryTransportEvent :utilizedTransportEquipment :mainCarriageTransportMovement
                     :consignment :actualOccurrenceDateTime :id
                     :categoryCode :identifier :registrationCountry :sequenceNumeric :carriedTransportEquipment
                     :transportModeCode :dangerousGoodsIndicator :usedTransportMeans}}})

(doall (for [[prefix {uri :namespace}] namespaces] (xml/alias-uri prefix uri)))

(def tag->namespaced-tag
  (reduce (fn [acc [prefix {tags :tags}]]
            (let [ns-string (->> prefix
                                 name
                                 symbol
                                 (get (ns-aliases *ns*))
                                 str)]
              (into acc
                    (for [tag tags]
                      [tag (keyword ns-string (name tag))])))) {} namespaces))

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
   :gateId :platformId :datasetId
   :categoryCode :identifier :registrationCountry :sequenceNumeric :carriedTransportEquipment
   :transportModeCode :dangerousGoodsIndicator :usedTransportMeans])

(defn rename-properties-object [rename object]
  (walk/postwalk (logic/when* map? #(plain/map-keys rename %)) object))

(defn replace-elements-in-tree [mapping tree]
  (walk/postwalk
   (fn [e]
     (if-let [replacement (get mapping e)]
       replacement
       e))
   tree))

(defn namespacefy-elements [xml]
  (replace-elements-in-tree tag->namespaced-tag xml))

(defn object->xml [element-key value]
  (->> value
       (rename-properties-object csk/->camelCaseKeyword)
       (fxml/object->xml+nowrap lists element-key)
       (fxml/reorder-children (comp #(collection/find-index (partial = %) order) first))
       namespacefy-elements))

(defn xml->object [xml]
  (->> xml
       (fxml/xml->object+nowrap lists)
       (rename-properties-object csk/->kebab-case-keyword)))

(def platform->edelivery-translation
  {:delivery-event :delivery-transport-event})

(defn remove-nil-delivery-event [consignment]
  (if-not (get-in consignment [:delivery-event :actual-occurrence-date-time])
    (dissoc consignment :delivery-event)
    consignment))

(defn translate-platform-data-to-edelivery [v]
  (->> v
       (mapv remove-nil-delivery-event)
       (replace-elements-in-tree platform->edelivery-translation)))

(defn rename-consignment-uil [v]
  (replace-elements-in-tree
   {::efti-id/uil ::efti/uil
    ::efti-id/gateId ::efti/gateId
    ::efti-id/platformId ::efti/platformId
    ::efti-id/datasetId ::efti/datasetId}
   v))

(defn consignments->xml [tag consignments]
  (->> consignments
       translate-platform-data-to-edelivery
       ((fn [n] {:consignments (mapv #(dissoc % :id) n)}))
       (object->xml tag)
       rename-consignment-uil
       namespacefy-elements))

(defn emit-xml-string [clj]
  (->> clj namespacefy-elements xml/sexp-as-element xml/emit-str))

(defn uil-response [consignment]
  (emit-xml-string
   (consignments->xml :uilResponse (maybe/fold [] vector consignment))))

(defn identifier-response [consignments]
  (emit-xml-string
   (consignments->xml :identifierResponse consignments)))

(def coerce-consignment
  (malli/coercer (schema/schema consignment-schema/Consignment) transformer))

(defn xml->consignments [xml]
  (->> xml xml->object :consignments (map coerce-consignment)))

(defn xml->consignment [query xml]
  (-> xml xml->object :consignments first
      (cond-> (subset/identifier? query) coerce-consignment)))

(defn uil-query->xml [query]
  (emit-xml-string
   [::efti-ed/uilQuery
    [::efti-id/uil
     [::efti-id/gateId (:gate-id query)]
     [::efti-id/platformId (:platform-id query)]
     [::efti-id/datasetId (:dataset-id query)]]
    [::efti-ed/subsetId (:subset-id query)]]))

(defn xml->uil-query [xml]
  (let [query (xml->object xml)]
    (assoc (:uil query) :subset-id (:subset-id query))))

(defn query->xml [query]
  (emit-xml-string
   [::efti-ed/identifierQuery
    [::efti-ed/identifier (:identifier query)]]))

(def coerce-query
  (malli/coercer (schema/schema consignment-schema/ConsignmentQuery) transformer))

(defn xml->query [xml] (->> xml (fxml/xml->object #{}) coerce-query))
