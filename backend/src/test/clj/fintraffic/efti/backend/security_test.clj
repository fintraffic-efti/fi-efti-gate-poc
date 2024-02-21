(ns fintraffic.efti.backend.security-test
  (:require [clojure.test :as t]
            [fintraffic.efti.backend.security :as security]))

(t/deftest log-safe-henkilotunnus-test
  (t/is (= "0101" (security/log-safe-ssn "0101")))
  (t/is (= "010101A****" (security/log-safe-ssn "010101A000A"))))
