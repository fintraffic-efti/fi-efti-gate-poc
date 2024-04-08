(ns fintraffic.common.xml
  (:require
    [clojure.string :as str]
    [fintraffic.common.debug :as debug]
    [fintraffic.common.logic :as logic])
  (:import (java.io ByteArrayInputStream InputStream)
           (java.nio.charset StandardCharsets)
           (javax.xml.parsers DocumentBuilderFactory)
           (org.w3c.dom Document)))

(def illegal-chars-in-xml #"[^\u0009\u000A\u000D\u0020-\uD7FF\uE000-\uFFFD\x{10000}-\x{10FFFF}]")

(defn sanitize
  "Replace illegal characters with the replacement character � (FFFD)."
  [txt] (str/replace txt illegal-chars-in-xml "�"))

(defn ^Document parse [^InputStream input]
  (-> (DocumentBuilderFactory/newInstance)
      (doto (.setNamespaceAware true))
      .newDocumentBuilder
      (.parse input)))

(defn txt->input-stream [^String txt]
  (-> txt (.getBytes StandardCharsets/UTF_8) ByteArrayInputStream.))