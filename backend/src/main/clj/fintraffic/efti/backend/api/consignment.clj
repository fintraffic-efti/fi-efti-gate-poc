(ns fintraffic.efti.backend.api.consignment
  (:require [fintraffic.efti.backend.api.response :as api-response]
            [fintraffic.efti.backend.service.consignment :as consignment-service]
            [fintraffic.efti.schema :as schema]
            [fintraffic.efti.schema.consignment :as consignment-schema]
            [fintraffic.efti.schema.role :as role]
            [malli.experimental.lite :as lmalli]
            [ring.util.response :as r]))

(def ConsignmentId (dissoc consignment-schema/UIL :gate-id :platform-id))

(defn uil [path config whoami]
  (assoc path :gate-id (:gate-id config)
              :platform-id (:id whoami)))

(def platform
  (with-bindings {#'lmalli/*options* schema/options}
    ["/consignments/:data-id"
      {:put {:summary    "Add new or update an existing consignment - gate subset"
             :access     role/platform?
             :parameters {:path ConsignmentId
                          :body consignment-schema/ConsignmentSave}
             :responses  {200 {:body nil?}}
             :handler    (fn [{{:keys [body path]} :parameters :keys [db whoami config]}]
                           (api-response/with-exceptions
                             #(do (consignment-service/save-consignment!
                                    db whoami (uil path config whoami) body)
                                  (r/status 204))
                             [{:constraint :consignment-country-start-id-fkey :response 400}
                              {:constraint :consignment-country-end-id-fkey :response 400}]))}

       :get {:summary    "Find an existing consignment by uil - gate subset"
             :access     role/platform?
             :parameters {:path ConsignmentId}
             :responses  {200 {:body consignment-schema/Consignment}}
             :handler    (fn [{{:keys [path]} :parameters :keys [db config whoami]}]
                           (api-response/get-response
                             (consignment-service/find-consignment db config (uil path config whoami))
                             (api-response/msg-404 "consignment" path)))}}]))

(def aap
  (with-bindings {#'lmalli/*options* schema/options}
    ["/consignments"
     [""
      {:get  {:summary    "Find consignments"
              :access     role/ca?
              :parameters {:query consignment-schema/ConsignmentQuery}
              :responses  {200 {:body (lmalli/vector consignment-schema/Consignment)}}
              :handler    (fn [{:keys [db config parameters]}]
                            (r/response (consignment-service/find-consignments db config (:query parameters))))}}]
     ["/:gate-id/:platform-id/:data-id"
      ["/identifiers"
       {:conflicting true
        :get         {:summary    "Find an existing consignment by uil - gate subset"
              :access     role/ca?
              :parameters {:path consignment-schema/UIL}
              :responses  {200 {:body consignment-schema/Consignment}}
              :handler    (fn [{{:keys [path]} :parameters :keys [db config]}]
                            (api-response/get-response
                              (consignment-service/find-consignment db config path)
                              (api-response/msg-404 "consignment" path)))}}]
      ["/:dataset-id"
       {:conflicting true
        :get {:summary    "Find an existing consignment by uil - full dataset or some subset from the platform"
              :access     role/ca?
              :parameters {:path consignment-schema/UILQuery}
              :responses  {200 {:body any?}}
              :handler    (fn [{{:keys [path]} :parameters :keys [db config]}]
                            (api-response/get-response
                              (consignment-service/find-consignment db config path)
                              (api-response/msg-404 "consignment" path)))}}]]]))