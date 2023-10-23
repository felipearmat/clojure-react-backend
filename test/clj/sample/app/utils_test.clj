(ns sample.app.utils-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [integrant.repl.state :as state]
    [sample.app.utils :as utils]))

(deftest test-contains-some?
  (testing "Should return true if any element in coll2 is in coll1"
    (is (utils/contains-some? [1 2 3] [3 4 5]))
    (is (utils/contains-some? [:a :b :c] [:b :d :e]))
    (is (utils/contains-some? #{:x :y :z} [:a :x :b])))

  (testing "Should return false if none of the elements in coll2 is in coll1"
    (is (not (utils/contains-some? [1 2 3] [4 5 6])))
    (is (not (utils/contains-some? [:a :b :c] [:d :e :f])))
    (is (not (utils/contains-some? #{:x :y :z} [:a :b :c])))))

(deftest test-validate-spec
  (testing "It returns the input if it's valid"
    (is (= "test" (utils/validate-spec string? "test"))))

  (testing "It throws an exception if input is not valid"
    (is (thrown? Exception (utils/validate-spec integer? "not-an-integer")))))

(deftest test-db-connector
  (testing "Should return a data-source if a database connection is established"
    (let [data-source :ds]
      (with-redefs [state/system {:db.sql/connection data-source}]
        (is (= data-source (utils/db-connector))))))

  (testing "It throws an exception if there isn't a database connection"
    (with-redefs [state/system {}]
      (is (thrown? Exception (utils/db-connector))))))
