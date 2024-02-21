(ns fintraffic.efti.reitit
  (:require [fintraffic.efti.backend.schema.common :as common-schema]
            [fintraffic.malli.collection :as malli-collection]
            [malli.transform :as malli-transform]
            [reitit.coercion.malli :as reitit-malli]))

(def coercion
  (reitit-malli/create
    (-> reitit-malli/default-options
        (assoc :options common-schema/options)
        (assoc-in [:transformers :string :default]
                  (malli-transform/transformer
                    (reitit-malli/-transformer
                      reitit-malli/string-transformer-provider
                      reitit-malli/default-options)
                    malli-collection/singleton->collection-transformer)))))
