(ns fintraffic.efti.backend.api.response
  (:require [ring.util.response :as r]
            [clojure.string :as str])
  (:import (clojure.lang ExceptionInfo)))

(defn msg-404 [entity & ids]
  (str (str/capitalize entity) ": " (str/join "/" ids) " does not exist."))

(defn get-response [body not-found]
  (if (nil? body)
    (r/not-found not-found)
    (r/response body)))

(defn ok|not-found [updated not-found]
  (if (or (nil? updated) (= updated 0))
    (r/not-found not-found)
    {:status 204 :body nil}))

(defn- matches-description? [error error-description]
  (let [matcher (dissoc error-description :response)
        matched-error (select-keys error (keys matcher))]
    (= matcher matched-error)))

(defn with-exceptions
  "Convert exceptions defined in error-descriptions to error responses.
  If exception data matches error description then it is returned as a response.
  The http response status is defined in error description.
  These exceptions must not contain any sensitive data."
  ([response-fn error-descriptions]
   (try
     (response-fn)
     (catch ExceptionInfo e
       (let [error (ex-data e)
             description (first (filter (partial matches-description? error)
                                        error-descriptions))]
         (if (nil? description)
           (throw e)
           {:status  (:response description)
            :headers {}
            :body    error}))))))

(defn created [path id]
  (r/created (str path "/" id) {:id id}))

(def unauthorized {:status 401 :body "Unauthorized"})
(def forbidden {:status 403 :body "Forbidden"})

(defn file-response-headers [content-type inline? filename]
  {"Content-Type" (or content-type "application/octet-stream")
   "Content-Disposition" (str (if inline? "inline" "attachment")
                              (str "; filename=\"" filename "\""))})

(defn csv-response-headers [filename inline?]
  (file-response-headers "text/csv" inline? filename))

;; This is only intended for asynchronous responses. Otherwise, use
;; the wrap-cache-control middleware.
(defn async-cache-headers [ttl-seconds]
  {"Cache-Control" (str "max-age=" ttl-seconds ",public")})

(defn file-response [body path-filename filename content-type inline? not-found]
  (cond
    (nil? body) (r/not-found not-found)
    (not= path-filename filename) (r/bad-request (str "Invalid filename: " path-filename))
    :else
    {:status 200
     :headers (file-response-headers content-type inline? filename)
     :body body}))

(defn csv-response [body path-filename filename not-found]
  (file-response body path-filename filename "text/csv; charset=utf-8" true not-found))

(defn pdf-response [body path-filename filename not-found]
  (file-response body path-filename filename "application/pdf" true not-found))

(defn xml-response [body path-filename filename inline? not-found]
  (file-response body path-filename filename "text/xml; charset=utf-8" inline? not-found))

(defn conflict [body]
  {:status 409
   :body body})

(defn ->xml-response [response]
  (assoc-in response [:headers "Content-Type"] "text/xml"))

(defn bad-request [body]
  {:status 400
   :headers {}
   :body body})

(defn internal-server-error [body]
  {:status 500
   :headers {}
   :body body})
