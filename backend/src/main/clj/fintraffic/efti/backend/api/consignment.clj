(ns fintraffic.efti.backend.api.consignment
  (:require [fintraffic.efti.backend.api.response :as api-response]
            [fintraffic.efti.backend.service.consignment :as consignment-service]
            [fintraffic.efti.schema :as schema]
            [fintraffic.efti.schema.consignment :as consignment-schema]
            [malli.experimental.lite :as lmalli]
            [ring.util.response :as r]))

(def ConsignmentId (dissoc consignment-schema/UIL :gate-url))

(defn assoc-gate-url [path config] (assoc path :gate-url (:gate-url config)))

(def routes
  (with-bindings {#'lmalli/*options* schema/options}
    ["/consignments/:platform-url/:data-id/gate"
      {:put {:summary    "Add new or update an existing consignment - gate subset"
             :access     any?
             :parameters {:path ConsignmentId
                          :body consignment-schema/ConsignmentSave}
             :responses  {200 {:body nil}}
             :handler    (fn [{{:keys [body path]} :parameters :keys [db whoami config]}]
                           (api-response/with-exceptions
                             #(do (consignment-service/save-consignment! db whoami (assoc-gate-url path config) body)
                                  (r/status 204))
                             [{:constraint :consignment/country-start-id-fkey :response 400}
                              {:constraint :consignment/country-end-id-fkey :response 400}]))}

       :get {:summary    "Find an existing consignment by uil - gate subset"
             :access     any?
             :parameters {:path ConsignmentId}
             :responses  {200 {:body consignment-schema/Consignment}}
             :handler    (fn [{{:keys [path]} :parameters :keys [db whoami config]}]
                           (api-response/get-response
                             (consignment-service/find-consignment db whoami (assoc-gate-url path config))
                             (api-response/msg-404 "consignment" path)))}}]))