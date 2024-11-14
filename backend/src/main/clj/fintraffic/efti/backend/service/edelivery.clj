(ns fintraffic.efti.backend.service.edelivery
  (:require [camel-snake-kebab.core :as csk]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [clojure.data.xml :as xml]
            [fintraffic.common.collection :as collection]
            [fintraffic.common.debug :as debug]
            [fintraffic.common.logic :as logic]
            [fintraffic.common.xml :as fxml]
            [fintraffic.efti.backend.db :as db]
            [fintraffic.efti.backend.db.query :as db-query]
            [fintraffic.efti.backend.exception :as exception]
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
  (:import (eu.efti.v1.codes CountryCode)
           (java.time ZoneId)
           (java.time.format DateTimeFormatterBuilder)
           (java.time.temporal ChronoField)
           (eu.efti.v1.edelivery ObjectFactory)))

(db/require-queries 'edelivery)

(def datatype-factory (javax.xml.datatype.DatatypeFactory/newInstance))

(def ^ObjectFactory object-factory (ObjectFactory.))

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
       first (maybe/require-some! (str "Field: " prop " getter does not exists for " clazz))))

(defn find-fields [^Class clazz]
  (concat (.getDeclaredFields clazz)
          (-> clazz .getSuperclass .getDeclaredFields)))

(defn find-clazz-field-type [^Class clazz prop]
  (->> clazz find-fields
       (filter #(= (.getName %) (name prop)))
       first (maybe/require-some! (str "Field: " prop " does not exists for " clazz))
       (.getType)))

(defn ->XMLGregorianCalendar [v]
  (cond
    (string? v)
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

    (instance? java.time.Instant v)
    (.newXMLGregorianCalendar datatype-factory
                              (doto (java.util.GregorianCalendar.)
                                (.setTimeInMillis (.toEpochMilli v))))))

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
            (try
              (.invoke setter o
                       (into-array [(condp = field-type
                                      javax.xml.datatype.XMLGregorianCalendar
                                      (->XMLGregorianCalendar v)

                                      java.lang.String
                                      (str v)

                                      java.math.BigInteger
                                      (java.math.BigInteger/valueOf v)

                                      v)]))
              (catch Exception e
                (IllegalArgumentException.
                  (str "Failed to set property: " prop
                       " for class: " clazz) e)))))))
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
  {:efti-ed "http://efti.eu/v1/edelivery"
   :efti-id "http://efti.eu/v1/consignment/identifier"
   :efti    "http://efti.eu/v1/consignment"})

(doall (for [[prefix uri] namespaces] (xml/alias-uri prefix uri)))

(def instant-format
  (-> (DateTimeFormatterBuilder.)
      (.appendPattern "uuuuMMddHHmm")
      ;;(.optionalStart)
      ;;(.appendLiteral \.)
      ;;(.appendFraction ChronoField/NANO_OF_SECOND, 1, 9, false)
      ;;(.optionalEnd)
      (.optionalStart)
      (.appendOffset "+HHmm", "Z")
      (.optionalEnd)
      ;;(.parseDefaulting ChronoField/NANO_OF_SECOND, 0)
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
  (sql/insert! db :ed-message (dissoc message :ref-to-message-id) db/default-opts))

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
   :utilizedTransportEquipments :usedTransportEquipment
   :carriedTransportEquipments :carriedTransportEquipment})

(def order
  [:uil :deliveryInformation :carrierAcceptanceDateTime :deliveryTransportEvent :utilizedTransportEquipment :mainCarriageTransportMovement
   :gateId :platformId :datasetId
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

(defn uil-response [consignment]
  (clj->xmlstring eu.efti.v1.edelivery.UILResponse
                  {:status 200
                   :consignment consignment}))

(defn consignment->ed-consignment [consignment]
  (as-> consignment $
        (debug/log $)
        (dissoc $ :id)
        (rename-properties-object
          (logic/when* #(= :identifier %) (constantly :id)) $)
        (rename-properties-object
          (logic/when* #(= :transport-mode-code %) (constantly :mode-code)) $)
        (rename-properties-object
          (logic/when* #(= :sequence-numeric %) (constantly :sequence-number)) $)
        (rename-properties-object
          (logic/when* #(= :utilized-transport-equipments %)
                       (constantly :used-transport-equipments)) $)
        (walk/postwalk
          (logic/when* (every-pred map-entry? #(-> % first (= :registration-country)))
                       (fn [[key country]] [key {:code (CountryCode/fromValue (:id country))}]))
          $)))

(defn identifier-response [consignments]
  (debug/log
    (clj->xmlstring eu.efti.v1.edelivery.IdentifierResponse
                    (rename-properties-object
                      csk/->camelCaseKeyword
                      {:status       200
                       :consignments (mapv consignment->ed-consignment consignments)}))))

(def coerce-consignment
  (malli/coercer (schema/schema consignment-schema/Consignment) transformer))

(defn ed-consignment->consignment [consignment]
  (->> consignment
       debug/log
       (walk/postwalk
         (logic/when* (every-pred map-entry? #(-> % first (= :registration-country)))
                      (fn [[key country]] [key (set/rename-keys country {:code :id})])))
       (rename-properties-object
         (logic/when* #(= :id %) (constantly :identifier)))
       (rename-properties-object
         (logic/when* #(= :mode-code %) (constantly :transport-mode-code)))
       (rename-properties-object
         (logic/when* #(= :sequence-number %) (constantly :sequence-numeric)))
       coerce-consignment))


(defn xml->consignments [xml]
  (->> xml xml->object :consignments (map ed-consignment->consignment)))

(defn xml->consignment [query xml]
  (-> xml xml->object :consignments first
      (cond-> (subset/identifier? query) ed-consignment->consignment)))

(defn uil-query->xml [query request-id]
  (debug/log
    (clj->xmlstring eu.efti.v1.edelivery.UILQuery
                    (rename-properties-object
                      csk/->camelCaseKeyword
                      {:request-id request-id
                       :subset-id  (:subset-id query)
                       :uil        (dissoc query :subset-id)}))))

(defn xml->uil-query [xml]
  (let [query (xml->object xml)]
    (assoc (:uil query) :subset-id (:subset-id query))))

(defn query->xml [query request-id]
  (clj->xmlstring eu.efti.v1.edelivery.IdentifierQuery
                  {:requestId request-id
                   :identifier {:value (:identifier query)}}))

(def coerce-query
  (malli/coercer (schema/schema consignment-schema/ConsignmentQuery) transformer))

(defn xml->query [xml] (->> xml (fxml/xml->object #{}) coerce-query))
