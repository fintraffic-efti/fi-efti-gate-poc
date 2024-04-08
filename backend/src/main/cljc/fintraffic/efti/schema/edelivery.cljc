(ns fintraffic.efti.schema.edelivery
  (:require [fintraffic.efti.schema :as schema]))

(def Message
  {:timestamp inst?
   :message-id string?
   :from-id string?
   :direction-id (schema/ForeignKey :ed-message-direction)
   :to-id [:maybe string?]
   :conversation-id [:maybe string?]
   :payload string?
   :content-type string?})