(ns fintraffic.malli.time
  (:require
    [malli.core :as malli]
    [tick.core :as tick]))

(defn ->safe-parser [f]
  (fn -parse [x]
    (if (string? x)
      (try
        (f x)
        (catch #?(:clj Throwable :cljs js/Error) _ x))
      x)))

(def default-date-types
  {:date     {:predicate tick/date? :parser tick/date :json-schema :date}
   :duration {:predicate tick/duration? :parser tick/duration :json-schema :duration}})


(defn time-schema
  [type]
  (when-let [props (get default-date-types type)]
    (let [{:keys [predicate parser json-schema]} props
          safe-parser (->safe-parser parser)
          -name (name type)
          message (str "Should be " -name)]
      (malli/-simple-schema
        {:type type
         :type-properties
         {:error/message    {:en message}
          :decode/json      {:enter safe-parser}
          :decode/string    {:enter safe-parser}
          :json-schema/type json-schema}
         :pred predicate}))))

(defn time-schemas []
  (reduce
    (fn [m k] (assoc m k (time-schema k)))
    {}
    (keys default-date-types)))
