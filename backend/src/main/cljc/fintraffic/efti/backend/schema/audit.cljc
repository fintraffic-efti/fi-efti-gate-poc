(ns fintraffic.efti.backend.schema.audit
  (:require [fintraffic.efti.backend.schema.common :as schema]))

(def Audit
  {:modifytime inst?
   :modifiedby-name string?})
