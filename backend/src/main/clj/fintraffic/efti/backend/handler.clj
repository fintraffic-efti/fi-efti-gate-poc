(ns fintraffic.efti.backend.handler
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [fintraffic.efti.backend.api.geo :as geo-api]
    [fintraffic.efti.backend.api.route :as api-route]
    [fintraffic.efti.backend.api.user :as user-api]
    [fintraffic.efti.backend.api.consignment :as consignment-api]
    [fintraffic.efti.backend.api.edelivery :as edelivery-api]
    [fintraffic.efti.backend.exception :as exception]
    [fintraffic.efti.backend.header-middleware :as header-middleware]
    [muuntaja.core :as muuntaja]
    [fintraffic.efti.backend.security :as security]
    [fintraffic.efti.backend.version :as version]
    [fintraffic.efti.reitit :as efti-reitit]
    [fintraffic.efti.schema.user :as user-schema]
    [reitit.dev.pretty :as pretty]
    [reitit.ring :as reitit-ring]
    [reitit.openapi :as reitit-openapi]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.muuntaja :as reitit-muuntaja]
    [reitit.ring.middleware.parameters :as reitit-parameters]
    [reitit.ring.middleware.exception :as reitit-exception]
    [reitit.spec :as reitit-spec]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.swagger :as reitit-swagger]))

(defn tag [tag routes]
  (walk/prewalk
    #(if (and (map? %) (contains? % :summary))
       (assoc % :tags #{tag}) %)
    routes))

(defn system-routes [config]
  [["/health"
    {:get {:summary   "Health check"
           :responses {200 {:body nil?}}
           :handler   (let [count (atom 0) max-count 10]
                        #(when (<= (swap! count inc) max-count)
                           (log/info "Health check #" @count "(max" max-count "logged)"))
                        {:status 200})}}]
   ["/version"
    {:get {:summary "System version"
           :responses {200 {:body any?}}
           :handler (version/version-handler config)}}]
   ["/headers"
    {:get {:summary "Endpoint for seeing request headers"
           :responses {200 {:body any?}}
           :handler (fn [{:keys [headers]}]
                      {:status 200
                       :body   headers})}}]])

(defn documentation-routes [config]
  ["/documentation" {:middleware [header-middleware/wrap-default-cache]}
    ["/openapi.json"
      {:get {:no-doc  true
             :openapi {:info {:title       "Efti Gateway API"
                              :description "Open api 3.1 definition for Efti Gateway"
                              :version     "0.0.1"}}
             :handler (reitit-openapi/create-openapi-handler)}}]
    ["/swagger.json"
      {:get {:no-doc  true
             :swagger {:info {:title       "Efti Gateway API"
                              :description "Open api 2.0 (swagger) definition for Efti Gateway"}}
             :handler (reitit-swagger/create-swagger-handler)}}]])

(defn routes [config]
  [["/api" {:middleware [header-middleware/wrap-default-cache]}
    (tag "System" (system-routes config))
    (tag "Documentation" (documentation-routes config))
    ["/v1/public" {:middleware [security/wrap-whoami-public-user
                                security/wrap-access
                                security/wrap-db-client]}
     (tag "Geo API" geo-api/routes)]
    (api-route/rename-api
      csk/->camelCaseKeyword csk/->kebab-case-keyword
      ["/v1/platform" {:middleware [#(security/wrap-certificate-whoami % user-schema/Platform)
                                    security/wrap-access
                                    security/wrap-db-client]}
       (tag "Platform user API" (user-api/whoami user-schema/Platform))
       (tag "Platform consignment API" consignment-api/platform)])
    (api-route/rename-api
      csk/->camelCaseKeyword csk/->kebab-case-keyword
      ["/v1/aap" {:middleware [#(security/wrap-certificate-whoami % user-schema/SystemUser)
                               security/wrap-access
                               security/wrap-db-client]}
       (tag "CA user API" (user-api/whoami user-schema/SystemUser))
       (tag "CA consignment API" consignment-api/aap)])
    ["/edelivery" {:middleware [#(security/wrap-whoami-static-user % user-schema/SystemUser
                                                                   (:edelivery user-schema/system))
                                security/wrap-access
                                security/wrap-db-client]}
     (tag "E-delivery API" edelivery-api/routes)]]])

(def route-opts
  {;; Uncomment line below to see diffs of requests in middleware chain
   ;;:reitit.middleware/transform dev/print-request-diffs
   :exception pretty/exception
   :validate  reitit-spec/validate
   :data      {:coercion   efti-reitit/coercion
               :muuntaja   (muuntaja/create
                             (update muuntaja/default-options :formats
                                     #(dissoc % "application/edn"
                                              "application/transit+json"
                                              "application/transit+msgpack")))
               :middleware [reitit-openapi/openapi-feature
                            header-middleware/wrap-default-content-type
                            reitit-parameters/parameters-middleware
                            reitit-muuntaja/format-negotiate-middleware
                            reitit-muuntaja/format-response-middleware
                            reitit-muuntaja/format-request-middleware
                            security/wrap-enforce-origin
                            #_security/wrap-enforce-content-type
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
          {:path   "/api/documentation"
           :url    "/api/documentation/openapi.json"
           :config {:validatorUrl     nil
                    :operationsSorter "alpha"}})
        ;; serve not found responses (404, 405, 406):
        (reitit-ring/create-default-handler)))
    {:middleware
     [reitit-exception/exception-middleware
      #_(when (-> config :web :csp) security/wrap-security-headers)]}))
