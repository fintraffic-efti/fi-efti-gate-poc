(ns fintraffic.malli.collection
  (:require [malli.transform :as malli-transform]))

(def nil->empty-collection-transformer
  (let [coders {:vector     #(if (nil? %) [] %)
                :sequential #(if (nil? %) '() %)
                :set        #(if (nil? %) #{} %)}]
    (malli-transform/transformer {:decoders coders :encoders coders})))

(def singleton->collection-transformer
  (let [coders {:vector     #(if (sequential? %) % [%])
                :sequential #(if (sequential? %) % '(%))
                :set        #(if (sequential? %) % #{%})}]
    (malli-transform/transformer {:decoders coders :encoders coders})))