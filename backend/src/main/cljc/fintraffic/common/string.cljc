(ns fintraffic.common.string
  (:require [clojure.string :as str])
  #?(:cljs (:require
             [goog.string :as gstr]
             ;; https://clojurescript.org/reference/google-closure-library#requiring-a-function
             ;; Sometimes symbols are not auto-included when requiring their parent namespace.
             ;; This happens when those symbols are in their own file and require specific inclusion
             goog.string.format)))
#?(:cljs
   (def format gstr/format))

(defn non-blank [txt]
  (when-not (str/blank? txt)
    txt))

(defn join-not-blank
  ([txts] (join-not-blank " " txts))
  ([separator txts]
   (->> txts
        (remove str/blank?)
        (str/join separator)
        non-blank)))

(defn remove-whitespace [txt] (str/replace txt #"\s" ""))

(defn truncate
  [txt max-length]
  (if (<= (count txt) max-length)
    txt
    (str (subs txt 0 (- max-length 1)) "…")))

(defn sample
  [txt index chars-before chars-after]
  (let [start (Math/max 0 (- index chars-before))
        end (Math/min (count txt) (+ index chars-after 1))]
    (subs txt start end)))

(defn input-stream
  ([s] (input-stream s "UTF-8"))
  ([s encoding]
   (-> s
       (.getBytes encoding)
       (java.io.ByteArrayInputStream.))))