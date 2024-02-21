(ns fintraffic.efti.schema.audit
  (:require [fintraffic.efti.schema.common :as schema]))

(def Audit
  {:modifytime inst?
   :modifiedby-name string?})
