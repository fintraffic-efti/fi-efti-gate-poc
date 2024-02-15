(ns fintraffic.io
  (:require [clojure.string :as str])
  (:import (java.io File)
           (java.nio.file Path)))

(defn ^ClassLoader class-loader [] (.getContextClassLoader (Thread/currentThread)))

(defn file? [^File file] (.isFile file))

(defn directory? [^File file] (.isDirectory file))

(defn ^Path path [^File file] (.toPath file))

(defn relative-path [^File from ^File to]
  (.relativize (path from) (path to)))

(defn path->str [^String separator ^Path path]
  (->> path .iterator iterator-seq (str/join separator)))