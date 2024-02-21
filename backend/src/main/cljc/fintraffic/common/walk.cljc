(ns fintraffic.common.walk
  (:require [clojure.walk :as clj-walk]))

(defn walk [inner outer form ignore?]
  (if (ignore? form)
    (outer form)
    (clj-walk/walk inner outer form)))

(defn postwalk
  "Like clojure.walk/postwalk, but with ability to ignore forms."
  [f ignore? form]
  (walk (partial postwalk f ignore?) f form ignore?))

(defn prewalk
  "Like clojure.walk/prewalk, but with ability to ignore forms.."
  [f ignore? form]
  (walk (partial prewalk f ignore?) identity (f form) ignore?))
