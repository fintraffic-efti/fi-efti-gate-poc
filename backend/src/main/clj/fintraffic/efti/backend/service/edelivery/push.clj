(ns fintraffic.efti.backend.service.edelivery.push
  (:require
    [clojure.data.xml :as xml]
    [fintraffic.common.xml :as fxml]
    [fintraffic.common.xpath :as xpath]
    [fintraffic.efti.backend.service.consignment :as consignment-service]
    [fintraffic.efti.backend.service.edelivery :as edelivery]
    [fintraffic.efti.backend.service.edelivery.ws :as edelivery-ws-service]
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.schema.edelivery :as edelivery-schema]
    [fintraffic.efti.schema.edelivery.message-direction :as message-direction]
    [fintraffic.efti.schema.edelivery.message-type :as message-type]
    [malli.core :as malli]
    [ring.util.codec :as ring-codec])
  (:import (java.nio.charset StandardCharsets)))

(def namespaces
  {:soap "http://www.w3.org/2003/05/soap-envelope"
   :eu "eu.domibus"})

(doall (for [[prefix uri] namespaces] (xml/alias-uri prefix uri)))

(def xml->message
  (xpath/converter
    {:timestamp "/soap:Envelope/soap:Header/eu:Messaging/UserMessage/MessageInfo/Timestamp/text()"
     :from-id "/soap:Envelope/soap:Header/eu:Messaging/UserMessage/PartyInfo/From/PartyId/text()"
     :to-id "/soap:Envelope/soap:Header/eu:Messaging/UserMessage/PartyInfo/To/PartyId/text()"
     :message-id "/soap:Envelope/soap:Header/eu:Messaging/UserMessage/MessageInfo/MessageId/text()"
     :conversation-id "/soap:Envelope/soap:Header/eu:Messaging/UserMessage/CollaborationInfo/ConversationId/text()"
     :payload "/soap:Envelope/soap:Body/eu:submitRequest/payload/value/text()"
     :content-type "/soap:Envelope/soap:Body/eu:submitRequest/payload/@mimeType"}
    namespaces))

(def coerce (malli/coercer (schema/schema edelivery-schema/Message) edelivery/transformer))

(defn submit-response [message-id]
  [::soap/Envelope [::soap/Body [::eu/submitResponse [:messageID message-id]]]])

(defn bytes->string [^bytes bytes] (String. bytes StandardCharsets/UTF_8))

(defn find-consignment [db config message xml]
  (->>
    xml edelivery/xml->uil
    (consignment-service/find-consignment-db db)
    (edelivery-ws-service/send-find-consignment-response-message! db config message)))

(def request-routes
  {:uil [message-type/find-consignment find-consignment]})

(defn process-request [message db config]
  (let [xml (-> message :payload xml/parse-str fxml/element->sexp)
        [request-type handler] (-> xml first request-routes)
        message (->> (or request-type message-type/response)
                     (assoc message :type-id)
                     (edelivery/add-message db))]
    (when (some? handler) (handler db config message xml))
    message))

(defn handle-message-xml [db config input]
  (-> input fxml/parse xml->message
      (update :payload (comp bytes->string ring-codec/base64-decode))
      (assoc :direction-id message-direction/in)
      coerce (process-request db config)
      :message-id submit-response xml/sexp-as-element xml/emit-str))