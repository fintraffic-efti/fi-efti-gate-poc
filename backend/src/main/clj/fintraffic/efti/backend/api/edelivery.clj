(ns fintraffic.efti.backend.api.edelivery
  (:require [fintraffic.efti.schema :as schema]
            [malli.experimental.lite :as lmalli]
            [ring.util.response :as ring-response]))

(def routes
  (with-bindings {#'lmalli/*options* schema/options}
    ["/messages"
     {:post {:summary    "Post edelivery message"
             :access     any?
             :parameters {:body any?}
             :responses  {200 {:body any?}}
             :handler    (fn [{:keys [db config body] :as request}]
                           (println (slurp body))
                           (ring-response/response
                             "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n
                               <soap:Body>\n
                               <ns2:submitResponse xmlns:ns2=\"http://org.ecodex.backend/1_1/\" xmlns:ns3=\"http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/\" xmlns:xmime=\"http://www.w3.org/2005/05/xmlmime\" xmlns:ns5=\"http://www.w3.org/2003/05/soap-envelope\">\n
                               <messageID>7bc148ee-f328-11ee-ada1-0242ac160008@edelivery.digital</messageID>\n
                               </ns2:submitResponse>\n
                             </soap:Body>\n</soap:Envelope>"))}}]))
