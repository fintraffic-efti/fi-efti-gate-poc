(ns fintraffic.efti.backend.security
  (:require
    [clojure.string :as str]
    [clojure.string]
    [clojure.tools.logging :as log]
    [fintraffic.common.map :as map]
    [fintraffic.efti.backend.api.response :as response]
    [fintraffic.efti.backend.schema.role :as role]
    [fintraffic.efti.backend.service.user :as whoami-service]
    [ring.util.response :as ring-response])
  (:import (java.net URI)))

(defn log-safe-ssn [henkilotunnus]
  (let [char-count (count henkilotunnus)]
    (->> (repeat "*")
         (take (- char-count (min 7 char-count)))
         (apply str)
         (str (subs henkilotunnus 0 (min 7 char-count))))))

(defn db-client
  [db user-id request]
  (-> db
      (assoc-in [:client :id] user-id)
      (assoc-in [:client :service-uri] (str "backend.efti" (:uri request)))))

(defn find+save-whoami! [db user request]
  (let [db (db-client db (:cognito role/system-users) request)
        whoami (whoami-service/find-whoami db (select-keys user [:email :ssn]))]
    (cond
      (nil? whoami)
      (whoami-service/add-user! db user)
      (not (map/submap? whoami (merge whoami user)))
      (whoami-service/update-user! db (:id whoami) user)
      :else (:id whoami))))

(defn wrap-whoami [handler login-url]
  (fn [request]
    (if-let [whoami
             (some->> request :session ::user-id
                      (whoami-service/find-whoami-by-id (:db request)))]
      (handler (assoc request :whoami whoami))
      (if (some? login-url)
        (ring-response/redirect login-url)
        response/unauthorized))))

(defn wrap-whoami-static-user [handler user-id]
  (fn [request]
    (handler (assoc request :whoami (whoami-service/find-whoami-by-id (:db request) user-id)))))

(defn wrap-access [handler]
  (fn [{:keys [request-method whoami] :as req}]
    (let [access (-> req :reitit.core/match :data (get request-method) :access)]
      (if (or (nil? access)
              (access whoami))
        (handler req)
        (do
          (log/warn "Current user did not satisfy the access predicate for route:"
                    {:method request-method
                     :url (-> req :reitit.core/match :template)
                     :whoami whoami})
          response/forbidden)))))

(defn wrap-db-client
  ([handler]
   (wrap-db-client handler "public"))
  ([handler default-id]
   (fn [{:keys [whoami] :as req}]
     (handler (update req :db db-client
                      (or (:id whoami) default-id) req)))))

(def csp-header "default-src 'self'; object-src 'none'; style-src 'self' 'unsafe-inline'; frame-src 'none'; media-src 'none'; child-src 'none'; upgrade-insecure-requests; frame-ancestors 'none'")

(defn wrap-security-headers [handler]
  (fn [request]
    (when-let [response (handler request)]
      (-> response
          (assoc-in [:headers "Content-Security-Policy"] csp-header)
          (assoc-in [:headers "Strict-Transport-Security"] "max-age=31536000")))))

(defn check-origin [req]
  ;; If there is a origin header, it must point to the same host as in the host header.
  ;; (1) In case of a form on a 3rd party site, origin will contain the
  ;; name of the 3rd party site, and we'll deny.
  ;; (2) In case of a XHR/fetch from our own SPA loaded from the our
  ;; origin, they will match. We'll allow.
  ;; (3) In case of non-browser API client, such as EV, curl for version
  ;; info, etc, the origin header will be absent. We'll allow.
  (let [origin-host (some-> req
                            (ring-response/get-header "origin")
                            (URI.)
                            (.getHost))
        server-name (:server-name req)
        allow? (or (nil? origin-host)
                   (= origin-host server-name))]
    allow?))

(defn wrap-enforce-origin [handler]
  (fn [req]
    (if (check-origin req)
      (handler req)
      (response/bad-request (str "origin mismatch - my server-name: " (:server-name req)
                                 "- my host header: " (ring-response/get-header req "host"))))))

(def content-type-whitelist
  #{"application/json"
    "application/transit+json"
    ;; For xml import
    "text/xml" "application/xml"
    ;; For EV messages
    "application/pkcs7-mime"})

(defn wrap-enforce-content-type [handler]
  (fn [req]
    (if (contains? #{:put :post :patch} (:request-method req))
      (let [req-content-type (ring-response/get-header req "content-type")]
        (if (some #(str/starts-with? req-content-type %) content-type-whitelist)
          (handler req) ;; proceed as normal
          ;; else - mutating method for some other content type
          (response/bad-request (str "For mutating HTTP methods, content type has to be one of: "
                                     (str/join ", " content-type-whitelist)))))
      ;; else (non-mutating method)
      (handler req))))
