(ns fintraffic.efti.schema.query
  (:require [fintraffic.efti.schema.common :as schema]))

(def Key+None
  [:or
   [:and keyword? [:enum :none]]
   schema/Key])
