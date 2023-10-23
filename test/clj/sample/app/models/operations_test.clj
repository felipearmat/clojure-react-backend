(ns sample.app.models.operations-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [sample.app.test-utils :as test-utils]
    [sample.app.models.operations :as ops]))

;; Create an operation before each test
(defn op-fixture [f]
  (ops/create-operation! "addition" 10.0)
  (f))

(use-fixtures :each test-utils/database-rollback op-fixture)

(deftest test-get-operations
  (testing "It retrieves operations based on 'where' conditions"
    (is (= 1 (count (ops/get-operations [:= :type "addition"]))))
    (is (= 0 (count (ops/get-operations [:= :type "non-existent"])))))

  (testing "It throws an exception if 'where' is not a vector"
    (is (thrown? Exception (ops/get-operations := :type "addition")))
    (is (thrown? Exception (ops/get-operations "addition")))))

(deftest test-delete-operation!
  (testing "It deletes an operation based on 'id'"
    (let [op-id (:id (last (ops/get-operations)))]
      (is (= 1 (ops/delete-operation! op-id)))
      (is (= 0 (count (ops/get-operations [:= :id op-id]))))
      (is (= 1 (count (ops/get-deleted-operations [:= :id op-id]))))))

  (testing "It throws an exception if 'id' is not an integer"
    (is (thrown? Exception (ops/delete-operation! "not-an-integer")))
    (is (thrown? Exception (ops/delete-operation! 3.14)))))

(deftest test-update-operations!
  (testing "It updates operations based on 'where' and 'set' conditions"
    (let [op-id (:id (last (ops/get-operations)))]
      (is (= 1 (ops/update-operations! [:= :id op-id] {:cost 20.0})))
      (is (= 1 (count (ops/get-operations [:= :id op-id]))))
      (is (= 20.0 (:cost (last (ops/get-operations [:= :id op-id])))))))

  (testing "It throws an exception if 'where' is not a vector"
    (is (thrown? Exception (ops/update-operations! "addition" {:cost 20.0}))))

  (testing "It throws an exception if 'set' is not a map"
    (is (thrown? Exception (ops/update-operations! [:= :type "addition"] "set 20.0")))))

(deftest test-create-operation!
  (testing "It creates a new operation"
    (ops/create-operation! "subtraction" 20.0)
    (let [new-id (:id (last (ops/get-operations)))]
      (is (integer? new-id))
      (is (= 1 (count (ops/get-operations [:= :id new-id]))))
      (is (= 20.0 (-> (last (ops/get-operations [:= :id new-id])) :cost)))))

  (testing "It throws an exception if 'type' is invalid"
    (is (thrown? Exception (ops/create-operation! "non-existent" 10.0)))
    (is (thrown? Exception (ops/create-operation! "invalid_type" 10.0))))

  (testing "It throws an exception if 'cost' is not a number"
    (is (thrown? Exception (ops/create-operation! "addition" "not-a-number")))
    (is (thrown? Exception (ops/create-operation! "addition" :not-a-number)))))
