(ns build
  (:require [clojure.java.io :as io]
            [clojure.tools.build.api :as b])
  (:import [java.nio.file Files]
           [com.sun.tools.xjc XJCFacade]))

(defn clean [_]
  (b/delete {:path "generated-src"})
  (b/delete {:path "target/xsdImport"}))

(def target-path
  (.getAbsolutePath (b/resolve-path "generated-src/xsd-import/java")))

(defn create-generated-src-dir []
  (Files/createDirectories (.toPath (io/file target-path)) (into-array java.nio.file.attribute.FileAttribute [])))

(def generated-sources-path "generated-src/xsd-import/java")

(defn xjc [_]
  (create-generated-src-dir)
  (XJCFacade/main (into-array [(.getAbsolutePath (b/resolve-path "src/main/resources/xsd/edelivery.xsd")) ; sourcefile
                               "-d"
                               (.getAbsolutePath (b/resolve-path generated-sources-path)) ; target path
                               "-no-header"])))

(defn classes [_]
  (b/javac {:src-dirs [generated-sources-path]
            :class-dir "target/xsdImport/classes"
            :basis (b/create-basis {:project "deps.edn"})}))
