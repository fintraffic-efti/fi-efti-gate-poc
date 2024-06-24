(ns fintraffic.common.xml-test
  (:require [clojure.test :as t]
            [fintraffic.common.xml :as fxml]))

(t/deftest object->xml+nowrap-test
  (t/is (= (fxml/object->xml+nowrap {} :t nil) [:t nil]))
  (t/is (= (fxml/object->xml+nowrap {} :t "test") [:t "test"]))
  (t/is (= (fxml/object->xml+nowrap {} :t {}) [:t]))
  (t/is (= (fxml/object->xml+nowrap {} :t {:a "test"}) [:t [:a "test"]]))
  (t/is (= (fxml/object->xml+nowrap {} :t {:a ["test"]}) [:t [:a "test"]]))
  (t/is (= (fxml/object->xml+nowrap {} :t {:a ["t1" "t2"]}) [:t [:a "t1"] [:a "t2"]]))
  (t/is (= (fxml/object->xml+nowrap {} :t {:a []}) [:t]))
  (t/is (= (fxml/object->xml+nowrap {} :t {:a [] :b "test"}) [:t [:b "test"]]))
  (t/is (= (fxml/object->xml+nowrap {} :t {:a [{:b "t1"} {:b "t2"}]})
           [:t [:a [:b "t1"]] [:a [:b "t2"]]]))
  (t/is (= (fxml/object->xml+nowrap {:a :b} :t {:a ["test"]}) [:t [:b "test"]])))

(t/deftest xml->object+nowrap-test
  (t/is (= (fxml/xml->object+nowrap {} [:t nil]) nil))
  (t/is (= (fxml/xml->object+nowrap {} [:t "test"]) "test"))
  (t/is (= (fxml/xml->object+nowrap {} [:t]) nil))
  (t/is (= (fxml/xml->object+nowrap {} [:t [:a "test"]]) {:a "test"}))
  (t/is (= (fxml/xml->object+nowrap {} [:t [:a [:b "b"] [:c "c"]]]) {:a {:b "b" :c "c"}})))