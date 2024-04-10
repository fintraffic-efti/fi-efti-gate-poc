(ns fintraffic.common.xpath
  (:refer-clojure :exclude [compile])
  (:require [clojure.set :as set]
            [fintraffic.common.debug :as debug]
            [fintraffic.common.map :as map])
  (:import (java.util Iterator)
           (javax.xml.namespace NamespaceContext)
           (javax.xml.xpath XPathConstants XPathExpression XPathFactory)))

(defn namespace-context [namespaces]
  (let [uri->prefix (set/map-invert namespaces)]
    (reify NamespaceContext
      (^String getNamespaceURI [_this ^String prefix] (-> prefix keyword namespaces))
      (^String getPrefix [_this ^String uri] (uri->prefix uri))
      (^Iterator getPrefixes [_this ^String uri] (-> uri uri->prefix vector .iterator)))))

(defn ^XPathExpression compile [^CharSequence expression namespaces]
  (-> (XPathFactory/newInstance)
      .newXPath
      (doto (.setNamespaceContext (namespace-context namespaces)))
      (.compile expression)))

(defn txt [xml ^XPathExpression xpath]
  (.evaluate xpath xml XPathConstants/STRING))

(defn compile-fn [^CharSequence expression namespaces]
  (let [xpath-exp (compile expression namespaces)]
    (fn [xml] (txt xml xpath-exp))))

(defn converter [description namespaces]
  (let [compiled (map/map-values #(compile % namespaces) description)]
    (fn [xml] (map/map-values #(txt xml %) compiled))))