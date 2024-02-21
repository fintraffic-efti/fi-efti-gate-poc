(ns fintraffic.efti.schema.query
  (:require
    [fintraffic.efti.schema :as schema]))

(defn Window [max-limit]
  {:limit  (schema/LimitedInt 1 max-limit)
   :offset int?})

(def ResultCount {:count int?})

(defn +None [type]
  [:or
   [:and keyword? [:enum :none]]
   type])
