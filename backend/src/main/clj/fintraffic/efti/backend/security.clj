(ns fintraffic.efti.backend.security
  (:require
    [clojure.string :as str]
    [clojure.string]
    [clojure.tools.logging :as log]
    [fintraffic.common.certificate :as certificate]
    [fintraffic.common.map :as map]
    [fintraffic.common.string :as fstr]
    [fintraffic.common.debug :as debug]
    [fintraffic.efti.backend.api.response :as response]
    [fintraffic.efti.backend.exception :as exception]
    [fintraffic.efti.backend.service.user :as whoami-service]
    [fintraffic.efti.schema.role :as role]
    [fintraffic.efti.schema.user :as user-schema]
    [ring.util.codec :as ring-codec]
    [ring.util.response :as ring-response])
  (:import (java.net URI)
           (java.security.cert X509Certificate)))

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
  (let [db (db-client db (:authentication role/system-users) request)
        whoami (whoami-service/find-whoami db (select-keys user [:email :ssn]))]
    (cond
      (nil? whoami)
      (whoami-service/add-user! db user)
      (not (map/submap? whoami (merge whoami user)))
      (whoami-service/update-user! db (:id whoami) user)
      :else (:id whoami))))

(defn wrap-session-whoami [handler login-url]
  (fn [request]
    (if-let [whoami
             (some->> request :session ::user-id
                      (whoami-service/find-whoami-by-id (:db request)))]
      (handler (assoc request :whoami whoami))
      (if (some? login-url)
        (ring-response/redirect login-url)
        response/unauthorized))))

(defn unauthorized!
  ([msg value] (unauthorized! some? msg value))
  ([predicate msg value]
   (if (predicate value)
     value (exception/throw-ex-info! :unauthorized msg))))

(defn wrap-certificate-whoami [handler schema]
  (fn [{:keys [db] :as request}]
    (let [db (db-client db (:authentication role/system-users) request)
          ^X509Certificate certificate
          (unauthorized!
           "Client certificate is missing."
           (some-> request :headers (get "x-amzn-mtls-clientcert-leaf")
                   ring-codec/url-decode fstr/input-stream certificate/read))
          platform-urn (unauthorized!
                        (complement str/blank?)
                        "Platform id urn is not defined in certificate."
                        (some->>
                         certificate .getSubjectAlternativeNames
                         (filter #(-> % second str/trim (str/starts-with? "urn:efti")))
                         first second str/trim))
          platform-id (-> platform-urn (str/split #":") (nth 3))
          whoami (unauthorized!
                  (str "Unable to find platform: " platform-id)
                  (whoami-service/find-whoami-by-platform-id db schema platform-id))]

      (handler (assoc request :whoami whoami)))))


(defn wrap-whoami-static-user [handler schema user-id]
  (fn [request]
    (handler (assoc request :whoami (whoami-service/find-whoami-by-id (:db request) schema user-id)))))

(defn wrap-whoami-public-user [handler]
  (fn [request] (handler (assoc request :whoami nil))))

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
