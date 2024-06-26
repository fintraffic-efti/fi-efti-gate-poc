(ns fintraffic.efti.backend.dev
  (:require
    [flathead.deep :as deep]
    [fintraffic.efti.backend.config :as config]))

;; These are only used as defaults for local development (not secret)
(def config
  (deep/deep-merge
    config/default-config
    {:host         "https://localhost:8280"
     :environment  :dev
     :gate-id      "fi1"
     :gate-ids     ["fi2"]
     :edelivery-ap "https://localhost:8443/services/backend"
     :db
     {:host          "localhost"
      :port          8432
      :username      "efti_gateway"
      :password      "efti"
      :database-name "efti_dev"}

     :http-server  {:port 8080}
     :nrepl        {:port 5959}}))
