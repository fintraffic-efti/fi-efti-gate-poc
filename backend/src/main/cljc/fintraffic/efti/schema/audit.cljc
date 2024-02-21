(ns fintraffic.efti.schema.audit
  (:require [fintraffic.efti.schema :as schema]))

(def Audit
  {:modifytime inst?
   :modifiedby-name string?})
