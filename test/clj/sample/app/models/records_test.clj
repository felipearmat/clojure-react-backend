(ns sample.app.models.records-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [sample.app.test-utils :as test-utils]
    [sample.app.models.operations :as operations]
    [sample.app.models.records :as records]
    [sample.app.models.users :as users]))

(def user_id (atom "test"))
(def op_id (atom 1))
(def user-email "test@mail.com")

;; Create a user, operations, and some records before each test
(defn base-fixtures [f]
  (users/create-user! user-email "Password@1")
  (operations/create-operation! "addition" 10.0)
  (reset! user_id (:id (last (users/get-users))))
  (reset! op_id (:id (last (operations/get-operations))))
  (records/create-record! {:operation_id @op_id
                           :user_id @user_id
                           :amount 50.0})
  (f))

(use-fixtures :each test-utils/database-rollback base-fixtures)

(deftest test-create-record!
  (let [valid-data {:operation_id @op_id
                    :user_id @user_id
                    :amount 50.0}]
    (testing "Create a new record with valid data"
      (is (= 1 (records/create-record! valid-data))))

    (testing "Shouldn't create a new record when missing operation_id"
      (is (thrown? Exception
            (records/create-record! (dissoc valid-data :operation_id)))))

    (testing "Shouldn't create a new record when missing user_id"
      (is (thrown? Exception
            (records/create-record! (dissoc valid-data :user_id)))))

    (testing "Shouldn't create a new record when missing amount"
      (is (thrown? Exception
            (records/create-record! (dissoc valid-data :amount)))))

    (testing "Shouldn't create a new record with invalid operation_id"
      (is (thrown? Exception
            (records/create-record! (assoc valid-data :operation_id "invalid")))))

    (testing "Shouldn't create a new record with invalid user_id"
      (is (thrown? Exception
            (records/create-record! (assoc valid-data :user_id "invalid")))))

    (testing "Shouldn't create a new record with invalid amount"
      (is (thrown? Exception
            (records/create-record! (assoc valid-data :amount -10.0)))))

    (testing "Operation response should be set to 'No response.' when empty"
      (let [new-record (first (records/get-records))]
        (is (= "No response." (:operation_response new-record)))))))

(deftest test-get-records
  (testing "Retrieve records based on 'where' conditions: user_id"
    (is (sequential? (records/get-records [:= :records.user_id @user_id]))))

  (testing "Retrieve records based on 'where' conditions: operation_id"
    (is (sequential? (records/get-records [:= :records.operation_id @op_id]))))

  (testing "Retrieve records based on 'where' conditions: amount"
    (is (sequential? (records/get-records [:= :records.amount 50.0]))))

  (testing "Retrieve records based on 'where' conditions: created_at"
    (let [created_at (:created_at (first (records/get-records)))]
      (is (sequential? (records/get-records [:= :records.created_at created_at]))))))
