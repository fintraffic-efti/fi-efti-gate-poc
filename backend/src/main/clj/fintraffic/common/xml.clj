(ns fintraffic.common.xml
  (:require
    [clojure.string :as str]))

(def illegal-chars-in-xml #"[^\u0009\u000A\u000D\u0020-\uD7FF\uE000-\uFFFD\x{10000}-\x{10FFFF}]")

(defn sanitize
  "Replace illegal characters with the replacement character � (FFFD)."
  [txt]
  (str/replace txt illegal-chars-in-xml "�"))
