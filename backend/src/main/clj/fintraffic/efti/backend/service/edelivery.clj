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
            [tick.core :as tick]
            [fintraffic.common.maybe :as maybe]
            [clojure.string :as str])
  (:import (java.time ZoneId)
           (java.time.format DateTimeFormatterBuilder)
           (java.time.temporal ChronoField)
           (eu.efti.v1.edelivery ObjectFactory)))

(db/require-queries 'edelivery)

(def datatype-factory (javax.xml.datatype.DatatypeFactory/newInstance))

(def object-factory (ObjectFactory.))

(def jaxb-class->factory-method
  (into {}
        (->> (.getMethods ObjectFactory)
             (filter #(= jakarta.xml.bind.JAXBElement (.getReturnType %)))
             (map (fn [method]
                    [(first (.getParameterTypes method)) method])))))

(defn find-setter [clazz prop]
  (->> (.getMethods clazz)
       (filter #(= (str "set" (str/lower-case (name prop))) (str/lower-case (.getName %))))
       first))

(defn find-list-getter [clazz prop]
  (->> (.getMethods clazz)
       (filter #(= (str "get" (str/lower-case (str/join (butlast (name prop))))) (str/lower-case (.getName %))))
       first))

(defn find-clazz-field-type [clazz prop]
  (->> (.getDeclaredFields clazz)
       (filter #(= (.getName %) (name prop)))
       first
       (.getType)))

(defn fill-jaxb [clazz node]
  (let [o (.newInstance clazz)]
    (doseq [[prop v] node]
      (let [setter (find-setter clazz prop)]
        (cond
          (nil? v) nil

          (sequential? v)
          (let [getter (find-list-getter clazz prop)
                ls (.invoke getter o (into-array []))
                generic-type (->> getter
                                  (.getAnnotatedReturnType)
                                  (.getType)
                                  (.getActualTypeArguments)
                                  first)]
            (doseq [e (map #(fill-jaxb generic-type %) v)]
              (.add ls e)))

          (map? v)
          (.invoke
           setter o
           (into-array [(fill-jaxb (find-clazz-field-type clazz prop) v)]))

          :else
          (let [field-type (find-clazz-field-type clazz prop)]
            (.invoke setter o
                     (into-array [(condp = field-type
                                    javax.xml.datatype.XMLGregorianCalendar
                                    (let [{:keys [year monthValue dayOfMonth hour minute second offset]}
                                          (bean (java.time.ZonedDateTime/parse v))]
                                      (.newXMLGregorianCalendar datatype-factory
                                                                year
                                                                monthValue
                                                                dayOfMonth
                                                                hour
                                                                minute
                                                                second
                                                                0
                                                                (/ (->> offset bean :totalSeconds) 60)))
                                    java.lang.String
                                    (str v)
                                    java.math.BigInteger
                                    (java.math.BigInteger/valueOf v)
                                    v)]))))))
    o))

(defn clj->xmlstring [jaxb-class clj-data]
  (let [marshaller (-> (jakarta.xml.bind.JAXBContext/newInstance
                        (into-array [jaxb-class]))
                       (.createMarshaller))
        string-writer (java.io.StringWriter.)]
    (.marshal marshaller
              (.invoke (jaxb-class->factory-method jaxb-class) object-factory
               (into-array [(fill-jaxb jaxb-class clj-data)]))
              string-writer)
    (str string-writer)))

(def namespaces
  {:efti-ed {:namespace "http://efti.eu/v1/edelivery"
             :tags #{:uilQuery :subsetId :uilResponse :identifierQuery :identifierResponse}}
   :efti-id {:namespace "http://efti.eu/v1/consignment/identifier"
             :tags #{:uil :gateId :platformId :datasetId}}
   :efti    {:namespace "http://efti.eu/v1/consignment"
             :tags #{:carrierAcceptanceDateTime :deliveryTransportEvent :utilizedTransportEquipment :mainCarriageTransportMovement
                     :consignment :actualOccurrenceDateTime :id :deliveryInformation
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
  [:uil :deliveryInformation :carrierAcceptanceDateTime :deliveryTransportEvent :utilizedTransportEquipment :mainCarriageTransportMovement
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
  {:deliveryEvent :deliveryTransportEvent})

(defn remove-nil-properties [consignment]
  (walk/postwalk
   (fn [v]
     (if (map? v)
       (reduce (fn [acc [k v]] (if v (assoc acc k v) acc)) {} v)
       v))
   consignment))

(defn translate-platform-data-to-edelivery [v]
  (->> v
       (mapv remove-nil-properties)
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
       ((fn [n] {:consignments (mapv #(dissoc % :id) n)}))
       (object->xml tag)
       rename-consignment-uil
       namespacefy-elements))

(defn emit-xml-string [clj]
  (->> clj namespacefy-elements xml/sexp-as-element xml/emit-str))

(defn uil-response [consignment]
  (clj->xmlstring eu.efti.v1.edelivery.UILResponse {:consignment consignment}))

(defn rename-consignment-to-identifier-namespace [xml]
  (replace-elements-in-tree
   {::efti/consignment ::efti-id/consignment
    ::efti/uil ::efti-id/uil
    ::efti/gateId ::efti-id/gateId
    ::efti/platformId ::efti-id/platformId
    ::efti/datasetId ::efti-id/datasetId
    ::efti/carrierAcceptanceDateTime ::efti-id/carrierAcceptanceDateTime
    ::efti/deliveryTransportEvent ::efti-id/deliveryTransportEvent
    ::efti/utilizedTransportEquipment ::efti-id/utilizedTransportEquipment
    ::efti/mainCarriageTransportMovement ::efti-id/mainCarriageTransportMovement
    ::efti/categoryCode ::efti-id/categoryCode
    ::efti/identifier ::efti-id/identifier
    ::efti/registrationCountry ::efti-id/registrationCountry
    ::efti/id ::efti-id/id
    ::efti/sequenceNumeric ::efti-id/sequenceNumeric
    ::efti/carriedTransportEquipment ::efti-id/carriedTransportEquipment
    ::efti/transportModeCode ::efti-id/transportModeCode
    ::efti/usedTransportMeans ::efti-id/usedTransportMeans} xml))

(defn translate-gate-data-to-edelivery [json]
  (->> json
       remove-nil-properties
       (replace-elements-in-tree {:delivery-event :delivery-transport-event})))

(defn identifier-response [consignments]
  (emit-xml-string
   (rename-consignment-to-identifier-namespace
    (consignments->xml :identifierResponse (translate-gate-data-to-edelivery consignments)))))

(def coerce-consignment
  (malli/coercer (schema/schema consignment-schema/Consignment) transformer))

(defn xml->consignments [xml]
  (->> xml xml->object :consignments (map coerce-consignment)))

(defn xml->consignment [query xml]
  (-> xml xml->object :consignments first
      (cond-> (subset/identifier? query) coerce-consignment)))

(defn uil-query->xml [query]
  (clj->xmlstring eu.efti.v1.edelivery.UILQuery
                  (rename-properties-object
                   csk/->camelCaseKeyword
                   (-> {:uil (dissoc query :subset-id)}
                       (assoc :subsetId (:subset-id query))))))

(defn xml->uil-query [xml]
  (let [query (xml->object xml)]
    (assoc (:uil query) :subset-id (:subset-id query))))

(defn query->xml [query]
  (clj->xmlstring eu.efti.v1.edelivery.IdentifierQuery
                  (select-keys query [:identifier])))

(def coerce-query
  (malli/coercer (schema/schema consignment-schema/ConsignmentQuery) transformer))

(defn xml->query [xml] (->> xml (fxml/xml->object #{}) coerce-query))
