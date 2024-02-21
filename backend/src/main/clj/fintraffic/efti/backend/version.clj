(ns fintraffic.efti.backend.version
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn slurp-resource [resource-name]
  (some-> resource-name io/resource slurp str/trim))

(defn version-handler [config]
  (let [version-response
        {:status 200
         :body
         (assoc (select-keys config [:environment :service])
           :git-revision (slurp-resource "git-revision")
           :build-date (slurp-resource "build-date"))}]
    (constantly version-response)))

