(ns fintraffic.efti.backend.api.edelivery
  (:require [fintraffic.common.debug :as debug]
            [fintraffic.efti.backend.api.response :as api-response]
            [fintraffic.efti.schema :as schema]
            [malli.experimental.lite :as lmalli]
            [ring.util.response :as ring-response]
            [fintraffic.efti.backend.service.edelivery.push :as edelivery-push-service]))

(def routes
  (with-bindings {#'lmalli/*options* schema/options}
    ["/messages"
     {:post {:summary    "Post edelivery message"
             :access     any?
             :parameters {:body any?}
             :responses  {200 {:body any?}}
             :handler    (fn [{:keys [db config body]}]
                           (->> body
                                (edelivery-push-service/handle-message-xml db config)
                                api-response/soap-response))}}]))
