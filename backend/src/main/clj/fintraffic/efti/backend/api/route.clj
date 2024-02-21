(ns fintraffic.efti.backend.api.route
  (:require
    [ring.util.response :as r]
    [malli.experimental.lite :as lmalli]
    [fintraffic.efti.schema :as schema]
    [fintraffic.efti.backend.api.response :as api-response]))

(defn find-all [name schema find-all-service]
  [(str "/" name)
   {:get {:summary   (str "Find all " name)
          :responses {200 {:body (lmalli/vector schema)}}
          :handler   (fn [{:keys [db]}]
                       (r/response (find-all-service db)))}}])

(defn update-value [{:keys [entity-name property summary
                            access body-schema error-409
                            update-service!]}]
  [(str "/" (name property))
   {:put {:summary    summary
          :access     access
          :parameters {:path schema/Id :body body-schema}
          :responses  {204 {:body nil?}
                       404 {:body string?}
                       409 (-> error-409 :schema (assoc :type :keyword))}
          :handler    (fn [{{{:keys [id]} :path :keys [body]} :parameters :keys [db]}]
                        (api-response/with-exceptions
                          #(api-response/ok|not-found
                             (update-service! db id body)
                             (api-response/msg-404 entity-name id))
                          [{:type     (:type error-409)
                            :response 409}]))}}])
