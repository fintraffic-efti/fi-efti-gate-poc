(ns fintraffic.efti.backend.api.route
  (:require
    [clojure.walk :as walk]
    [fintraffic.common.logic :as logic]
    [fintraffic.common.map :as map]
    [fintraffic.efti.backend.api.response :as api-response]
    [fintraffic.efti.schema :as schema]
    [fintraffic.malli.map :as malli-map]
    [flathead.plain :as plain]
    [ring.util.response :as r]))

(defn find-all [name schema find-all-service]
  [(str "/" name)
   {:get {:summary   (str "Find all " name)
          :responses {200 {:body (schema/vector schema)}}
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

(defn rename-malli [rename malli]
  (->> malli schema/schema (malli-map/rename-entries rename)))


(defn rename-responses [rename responses]
  (walk/postwalk
    (logic/when*
      (every-pred map-entry? #(= (key %) :body))
      #(update % 1 (partial rename-malli rename)))
    responses))

(defn rename-properties-object [rename object]
  (walk/postwalk (logic/when* map? #(plain/map-keys rename %)) object))

(defn rename-properties-middleware [->external-name ->internal-name handler]
  (fn [request]
    (-> request
        (map/update-in-if
          [:parameters :body] map/defined?
          (partial rename-properties-object ->internal-name))
        handler
        (->> (rename-properties-object ->external-name)))))

(defn rename-api [->external-name ->internal-name route]
  (->> route
       (walk/postwalk
         (logic/when*
           (every-pred map-entry? #(= (key %) :parameters))
           #(map/update-in-if % [1 :body] map/defined? (partial rename-malli ->external-name))))
       (walk/postwalk
         (logic/when*
           (every-pred map-entry? #(= (key %) :responses))
           #(update % 1 (partial rename-responses ->external-name))))
       (walk/postwalk
         (logic/when*
           (every-pred map-entry? #(= (key %) :handler))
           #(update % 1 (partial rename-properties-middleware ->external-name ->internal-name))))))


