(ns fintraffic.efti.backend.log-test
  (:require [clojure.string :as str]
            [clojure.test :as t]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayOutputStream PrintStream)))

(t/deftest ^:eftest/synchronized test-system-out-logs
  (let [^PrintStream console System/out
        ^ByteArrayOutputStream output (ByteArrayOutputStream.)
        ^PrintStream stream (PrintStream. output)]
    (try
      (System/setOut stream)
      (log/error "test-423952203349-test")
      (finally
        (System/setOut console)))

    (t/is (str/includes? output "test-423952203349-test"))))


