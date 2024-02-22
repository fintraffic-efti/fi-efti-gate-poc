(ns fintraffic.efti.backend.api.consignment
  (:require [fintraffic.efti.backend.api.response :as api-response]
            [fintraffic.efti.backend.service.consignment :as consignment-service]
            [fintraffic.efti.schema :as schema]
            [fintraffic.efti.schema.consignment :as consignment-schema]
            [malli.experimental.lite :as lmalli]
            [ring.util.response :as r]))

(def routes
  (with-bindings {#'lmalli/*options* schema/options}
    ["/consignments/:gate-url/:platform-url/:data-id/gate"
      {:put {:summary    "Add new or update an existing consignment - gate subset"
             :access     any?
             :parameters {:path consignment-schema/UIL
                          :body consignment-schema/ConsignmentSave}
             :responses  {200 {:body nil}}
             :handler    (fn [{{:keys [body path]} :parameters :keys [db whoami]}]
                           (api-response/with-exceptions
                             #(do (consignment-service/save-consignment! db whoami path body)
                                  (r/status 204))
                             [{:constraint :consignment/country-start-id-fkey :response 400}
                              {:constraint :consignment/country-end-id-fkey :response 400}]))}

       :get {:summary    "Find an existing consignment by uil - gate subset"
             :access     any?
             :parameters {:path consignment-schema/UIL}
             :responses  {200 {:body consignment-schema/Consignment}}
             :handler    (fn [{{:keys [path]} :parameters :keys [db whoami]}]
                           (api-response/get-response
                             (consignment-service/find-consignment db whoami path)
                             (api-response/msg-404 "consignment" path)))}}]))