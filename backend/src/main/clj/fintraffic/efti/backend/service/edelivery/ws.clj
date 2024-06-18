(ns fintraffic.efti.backend.service.edelivery.ws
  (:require
    [clj-http.client :as http]
    [clojure.data.xml :as xml]
    [fintraffic.common.xml :as fxml]
    [fintraffic.common.xpath :as xpath]
    [fintraffic.efti.backend.service.edelivery :as edelivery-service]
    [fintraffic.efti.schema.edelivery.message-direction :as message-direction]
    [fintraffic.efti.schema.edelivery.message-type :as message-type]
    [ring.util.codec :as ring-codec]
    [tick.core :as tick])
  (:import (java.nio.charset StandardCharsets)))

(def namespaces
  {:soap "http://www.w3.org/2003/05/soap-envelope"
   :eb "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/"
   :eu "http://org.ecodex.backend/1_1/"})

(doall (for [[prefix uri] namespaces] (xml/alias-uri prefix uri)))

(defn party-id [id] [::eb/PartyId {:type "urn:oasis:names:tc:ebcore:partyid-type:unregistered"} id])

(defn submit-request-xml [{:keys [from-id to-id conversation-id payload content-type]}]
  [::soap/Envelope
   [::soap/Header
    [::eb/Messaging
     [::eb/UserMessage {:mpc "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC"}
      [::eb/PartyInfo
       [::eb/From (party-id from-id)
        [::eb/Role "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator"]]
       [::eb/To (party-id to-id)
        [::eb/Role "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder"]]]
      [::eb/CollaborationInfo
       [::eb/Service {:type "tc1"} "bdx:noprocess"]
       [::eb/Action "TC1Leg1"]
       [::eb/ConversationId conversation-id]]
      [::eb/MessageProperties
       [::eb/Property {:name "originalSender"} "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1"]
       [::eb/Property {:name "finalRecipient"} "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4"]]
      [::eb/PayloadInfo
       [::eb/PartInfo {:href "cid:message"}
        [::eb/PartProperties [::eb/Property {:name "MimeType"} "text/xml"]]]]
      [::eb/ProcessingType "PUSH"]]]]
   [::soap/Body
    [::eu/submitRequest
     [:payload {:payloadId "cid:message", :contentType content-type}
      [:value (-> payload (.getBytes StandardCharsets/UTF_8) ring-codec/base64-encode)]]]]])

(def message-id (xpath/compile-fn "/soap:Envelope/soap:Body/eu:submitResponse/messageID/text()" namespaces))

(defn post-request [xml]
  {:body (-> xml xml/sexp-as-element xml/emit-str)
   :content-type "application/soap+xml;charset=UTF-8"
   :insecure? true
   :as :stream})

(defn send-message! [db config message]
  (let [message
        (-> message
            (assoc
              :timestamp (tick/now)
              :from-id (:gate-id config)
              :content-type "text/xml"
              :direction-id message-direction/out)
            (update :payload (comp xml/emit-str xml/sexp-as-element)))]
    (->> message submit-request-xml post-request
         (http/post (:edelivery-ap config))
         :body fxml/parse message-id (assoc message :message-id)
         (edelivery-service/add-message db))))

(defn send-find-consignment-message! [db config conversation-id uil]
  (send-message! db config {:to-id           (:gate-id uil)
                            :type-id         message-type/find-consignment
                            :conversation-id conversation-id
                            :payload         (edelivery-service/uil->xml uil)}))

(defn send-find-consignments-message! [db config conversation-id gate-id query]
  (send-message! db config {:to-id           gate-id
                            :type-id         message-type/find-consignments
                            :conversation-id conversation-id
                            :payload         (edelivery-service/query->xml query)}))

(defn send-find-consignments-response-message! [db config request consignments]
  (send-message! db config {:to-id           (:from-id request)
                            :type-id         message-type/find-consignments
                            :conversation-id (:conversation-id request)
                            :payload         (edelivery-service/consignments->xml consignments)}))