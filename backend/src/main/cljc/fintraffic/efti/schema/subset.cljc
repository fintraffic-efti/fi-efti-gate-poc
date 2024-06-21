(ns fintraffic.efti.schema.subset)

(def identifier "identifier")

(defn identifier? [m] (or (= (:subset-id m) identifier)
                          (nil? (:subset-id m))))
