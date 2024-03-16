(ns fintraffic.efti.backend.handler
  (:require
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [fintraffic.efti.backend.api.geo :as geo-api]
    [fintraffic.efti.backend.api.user :as user-api]
    [fintraffic.efti.backend.api.consignment :as consignment-api]
    [fintraffic.efti.backend.exception :as exception]
    [fintraffic.efti.backend.header-middleware :as header-middleware]
    [fintraffic.efti.backend.muuntaja :as muuntaja]
    [fintraffic.efti.backend.security :as security]
    [fintraffic.efti.backend.version :as version]
    [fintraffic.efti.reitit :as efti-reitit]
    [reitit.dev.pretty :as pretty]
    [reitit.ring :as reitit-ring]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.muuntaja :as reitit-muuntaja]
    [reitit.ring.middleware.parameters :as reitit-parameters]
    [reitit.ring.middleware.exception :as reitit-exception]
    [reitit.spec :as rs]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]))

(defn tag [tag routes]
  (walk/prewalk
    #(if (and (map? %) (contains? % :summary))
       (assoc % :tags #{tag}) %)
    routes))

(defn system-routes [config]
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title       "Efti Gateway API"
                            :description "Open api definition for Efti Gateway"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/health"
    {:get {:summary "Health check"
           :handler (let [count (atom 0) max-count 10]
                      (fn [req]
                        (when (<= (swap! count inc) max-count)
                          (log/info "Health check #" @count "(max" max-count "logged)"))
                        {:status 200}))}}]
   ["/version"
    {:get {:summary "System version"
           :handler (version/version-handler config)}}]
   ["/headers"
    {:get {:summary "Endpoint for seeing request headers"
           :handler (fn [{:keys [headers]}]
                      {:status 200
                       :body   headers})}}]])

(defn routes [config]
  [["/api" {:middleware [header-middleware/wrap-default-cache]}
    (tag "System" (system-routes config))
    ["/v1/public" {:middleware [#(security/wrap-whoami-static-user % nil)
                                security/wrap-access
                                security/wrap-db-client]}
     (tag "Geo API" geo-api/routes)]
    ["/v1/platform" {:middleware [security/wrap-certificate-whoami
                                  security/wrap-access
                                  security/wrap-db-client]}
     (tag "Platform user API" user-api/whoami)
     (tag "Platform consignment API" consignment-api/platform)]
    ["/v1/aap" {:middleware [#(security/wrap-whoami-static-user % nil)
                             security/wrap-access
                             security/wrap-db-client]}
     (tag "CA user API" user-api/whoami)
     (tag "CA consignment API" consignment-api/aap)]]])

(def route-opts
  {;; Uncomment line below to see diffs of requests in middleware chain
   ;;:reitit.middleware/transform dev/print-request-diffs
   :exception pretty/exception
   :validate  rs/validate
   :data      {:coercion   efti-reitit/coercion
               :muuntaja   muuntaja/instance
               :middleware [header-middleware/wrap-default-content-type
                            swagger/swagger-feature
                            reitit-parameters/parameters-middleware
                            reitit-muuntaja/format-negotiate-middleware
                            reitit-muuntaja/format-response-middleware
                            reitit-muuntaja/format-request-middleware
                            security/wrap-enforce-origin
                            security/wrap-enforce-content-type
                            exception/exception-middleware
                            coercion/coerce-response-middleware
                            coercion/coerce-request-middleware
                            multipart/multipart-middleware
                            exception/log-4xx]}})

(defn router [config] (reitit-ring/router (routes config) route-opts))

(defn handler [config]
  (reitit-ring/ring-handler
    (router config)
    (->
      (reitit-ring/routes
        (swagger-ui/create-swagger-ui-handler
          {:path             "/api/documentation"
           :url              "/api/swagger.json"
           :config           {:validationUrl nil}
           :operationsSorter "alpha"})
        ;; serve not found responses (404, 405, 406):
        (reitit-ring/create-default-handler)))
    {:middleware
     [reitit-exception/exception-middleware
      #_(when (-> config :web :csp) security/wrap-security-headers)]}))
