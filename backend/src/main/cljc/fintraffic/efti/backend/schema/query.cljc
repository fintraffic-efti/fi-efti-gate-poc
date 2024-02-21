(ns fintraffic.efti.backend.schema.query
  (:require [fintraffic.efti.backend.schema.common :as schema]))

(def Key+None
  [:or
   [:and keyword? [:enum :none]]
   schema/Key])
