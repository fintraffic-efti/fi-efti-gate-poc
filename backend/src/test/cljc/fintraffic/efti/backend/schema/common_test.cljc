(ns fintraffic.efti.schema.common-test
  (:require [clojure.test :as t]
            [fintraffic.efti.schema.common :as schema]))

(t/deftest valid-email?-test
  (t/is (schema/valid-email? "test@example.com"))
  (t/is (not (schema/valid-email? "test@example")))
  (t/is (not (schema/valid-email? "test@exam@ple.com")))
  (t/is (schema/valid-email? "testäexam@ple.com"))
  (t/is (schema/valid-email? "Test@example.com"))
  (t/is (not (schema/valid-email? "Test@exa$mple.com")))
  (t/is (schema/valid-email? "Test.Example@example.com"))
  (t/is (schema/valid-email? "Test.Example@Example.com")))