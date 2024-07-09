(ns fintraffic.common.xml
  (:require
    [clojure.data.xml :as xml]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [fintraffic.common.logic :as logic]
    [fintraffic.common.map :as map])
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
      (vec (concat [(keyword (name (:tag element))) (:attrs element)] children)))))

(defn remove-attributes [xml]
  (walk/postwalk (logic/when* vector? (partial filterv (complement map?))) xml))

(defn child-element? [xml] (-> xml second vector?))

(defn object->xml [element-key list-element->item-key value]
  (->>
    (cond (map? value)
          (map (fn [[key value]] (object->xml key list-element->item-key value)) value)
          (and (sequential? value) (-> element-key list-element->item-key some?))
          (map #(object->xml (list-element->item-key element-key) list-element->item-key %) value)
          :else [value])
    (cons element-key) vec))

(defn xml->object [list-element? xml]
  (let [xml (remove-attributes xml)]
    (cond (-> xml first list-element?)
          (or (mapv #(xml->object list-element? %) (rest xml)) [])
          (child-element? xml)
          (into {} (map #(vector (first %) (xml->object list-element? %))) (rest xml))
          :else (second xml))))

(defn object->xml+nowrap [list-key->element-key element-key value]
  (let [wrap (logic/unless* (some-fn #(-> % first sequential?) empty?) vector)]
    (cond (map? value)
          (->> value
               (mapcat (fn [[key value]] (wrap (object->xml+nowrap list-key->element-key (or (list-key->element-key key) key) value))))
               (cons element-key) vec)
          (sequential? value)
          (mapv #(object->xml+nowrap list-key->element-key element-key %) value)
          :else [element-key value])))

(defn xml->object+nowrap [list-key->element-key xml]
  (let [xml (remove-attributes xml)
        element-key->list-key (into {} (map (fn [[key value]] [value key])) list-key->element-key)]
    (cond (-> xml first sequential?)
          (map (partial xml->object+nowrap list-key->element-key) xml)
          (child-element? xml)
          (->> (rest xml)
               (group-by first)
               (map/map-values (partial map (partial xml->object+nowrap list-key->element-key)))
               (into {} (map (fn [[key value]]
                               (if-let [list-key (element-key->list-key key)]
                                 [list-key value] [key (first value)])))))
          :else (second xml))))

(defn reorder-children [sort-value xml]
  (walk/postwalk (logic/when* (every-pred
                                sequential?
                                child-element?)
                              #(->> (rest %)
                                    (sort-by sort-value)
                                    (cons (first %)) vec))
                 xml))
