(ns fintraffic.common.map-test
  (:require [clojure.test :as t]
            [fintraffic.common.map :as map]))

(def test-map-1 {:a {:b {:d :foobar}
                     :c :baz}})
(def test-map-2 {:a [:foo :bar {:d :foobarbaz}]
                 :b {:c :foobar}})

(t/deftest paths-test
  (t/is (= (map/paths nil) []))
  (t/is (= (map/paths []) []))
  (t/is (= (sort (map/paths test-map-1)) [[:a :c] [:a :b :d]]))
  (t/is (= (sort (map/paths test-map-2)) [[:a 0] [:a 1] [:b :c] [:a 2 :d]])))

(t/deftest dissoc-in-empty
  (t/is (= nil (map/dissoc-in nil nil)))
  (t/is (= nil (map/dissoc-in nil [])))
  (t/is (= {} (map/dissoc-in {} [])))
  (t/is (= nil (map/dissoc-in nil [:a])))
  (t/is (= {} (map/dissoc-in {} [:a]))))

(defn test-dissoc-in-flat-map [m key]
  (t/is (= (dissoc m key) (map/dissoc-in m [key]))))

(t/deftest dissoc-in-flat-map
  (test-dissoc-in-flat-map {:a 1} :a)
  (test-dissoc-in-flat-map {:a 1 :b 1} :a)
  (test-dissoc-in-flat-map {:a 1 :b 1} :xxx))

(t/deftest dissoc-in-nested-map
  (let [test-map {:a 1 :b {:c 2 :d {:e 4}}}]
    (t/is (= {:a 1} (map/dissoc-in test-map [:b])))
    (t/is (= test-map (map/dissoc-in test-map [:xxx :yyy :zzz])))
    (t/is (= {:a 1 :b {:d {:e 4}}} (map/dissoc-in test-map [:b :c])))
    (t/is (= {:a 1 :b {:c 2}} (map/dissoc-in test-map [:b :d])))
    (t/is (= {:a 1 :b {:c 2 :d {}}} (map/dissoc-in test-map [:b :d :e])))))
