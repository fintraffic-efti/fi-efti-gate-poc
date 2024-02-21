(ns fintraffic.common.maybe
  #?(:clj (:import (java.util Objects))))

(defn map* [fn optional] (when (some? optional) (fn optional)))

(defn filter* [predicate optional]
  (when (and (some? optional) (predicate optional)) optional))

(defn non-empty [optional]
  (when-not (empty? optional) optional))

(defn fold [default fn optional]
  (if (some? optional) (fn optional) default))

(defn lift1 [original-fn] #(map* original-fn %))
(defn lift2 [original-fn] #(when (and (some? %1) (some? %2)) (original-fn %1 %2)))

#?(:clj
   (defn require-some!
     ([value] (Objects/requireNonNull value))
     ([^String message value] (Objects/requireNonNull value message))))
