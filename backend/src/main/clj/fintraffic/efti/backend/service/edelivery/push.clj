(ns fintraffic.efti.backend.service.edelivery.push
  (:require
    [clojure.data.xml :as xml]
    [fintraffic.common.xml :as fxml]
    [fintraffic.common.xpath :as xpath]
    [fintraffic.efti.backend.service.edelivery :as edelivery]
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.schema.edelivery :as edelivery-schema]
    [fintraffic.efti.schema.edelivery.message-direction :as message-direction]
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

(defn bytes->string [^"[[B" bytes] (String. bytes StandardCharsets/UTF_8))

(defn add-message-xml [db input]
  (-> input fxml/parse xml->message
      (update :payload (comp bytes->string ring-codec/base64-decode))
      (assoc :direction-id message-direction/in) coerce
      (->> (edelivery/add-message db)) :message-id
      submit-response xml/sexp-as-element xml/emit-str))