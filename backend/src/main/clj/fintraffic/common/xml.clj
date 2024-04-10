(ns fintraffic.common.xml
  (:require
    [clojure.data.xml :as xml]
    [clojure.string :as str]
    [clojure.walk :as walk]
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

(defn element->sexp [element]
  (let [children (->> element :content (filter xml/element?) (map element->sexp))]
    (if (empty? children)
      (let [value (->> element :content first)]
        (if (str/blank? value)
          [(:tag element) (:attrs element)]
          [(:tag element) (:attrs element) value]))
      (vec (concat [(:tag element) (:attrs element)] children)))))

(defn object->xml [element-key list-element->item-key value]
  (->>
    (cond (map? value)
          (map (fn [[key value]] (object->xml key list-element->item-key value)) value)
          (and (sequential? value) (-> element-key list-element->item-key some?))
          (map #(object->xml (list-element->item-key element-key) list-element->item-key %) value)
          :else [value])
    (cons element-key) vec))

(defn xml->object [list-element? xml]
  (let [xml (walk/postwalk (logic/when* vector? (partial filterv (complement map?))) xml)]
    (cond (and (-> xml second vector?) (-> xml first list-element?))
          (mapv #(xml->object list-element? %) (rest xml))
          (-> xml second vector?)
          (into {} (map #(vector (first %) (xml->object list-element? %))) (rest xml))
          :else (second xml))))