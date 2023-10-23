(ns sample.app.models.credits-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [sample.app.test-utils :as test-utils]
    [sample.app.models.credits :as credits]
    [sample.app.models.users :as users]))

(def user-id (atom "test"))
(def user-email "test@mail.com")

;; Create a user and some credits before each test
(defn base-fixtures [f]
  (users/create-user! user-email "Password@1")
  (reset! user-id (:id (first (users/get-users))))
  (credits/add-credit! @user-id 50.0)
  (f))

(use-fixtures :each test-utils/database-rollback base-fixtures)

(deftest test-add-credit!
  (testing "Add credit with valid data"
    (is (= 1 (credits/add-credit! @user-id 100.0)))
    (is (= 2 (count (credits/get-credits [:= :credits.user_id @user-id])))))

  (testing "Shouldn't add credit when missing user_id"
    (is (thrown? Exception
            (credits/add-credit! nil 100.0))))

  (testing "Shouldn't add credit when missing amount"
    (is (thrown? Exception
            (credits/add-credit! @user-id nil))))

  (testing "Shouldn't add credit with invalid user_id"
    (is (thrown? Exception
            (credits/add-credit! "invalid" 100.0))))

  (testing "Shouldn't add credit with invalid amount"
    (is (thrown? Exception
            (credits/add-credit! @user-id -10.0)))))

(deftest test-get-credits
  (testing "Retrieve credits based on 'where' conditions: user_id"
    (is (= 1 (count (credits/get-credits [:= :credits.user_id @user-id])))))

  (testing "Retrieve credits based on 'where' conditions: value"
    (is (sequential? (credits/get-credits [:= :credits.value 50.0]))))

  (testing "Retrieve credits based on 'where' conditions: created_at"
    (let [created_at (:created_at (first (credits/get-credits)))]
      (is (sequential? (credits/get-credits [:= :credits.created_at created_at])))))

  (testing "Retrieve credits with invalid 'where' conditions"
    (is (thrown? Exception
            (credits/get-credits [:= :credits.value "invalid"])))))

(deftest test-get-deleted-credits
  (testing "Retrieve deleted credits based on 'where' conditions: user_id"
    (credits/delete-credit! (:id (first (credits/get-credits [:= :credits.user_id @user-id]))))
    (is (sequential? (credits/get-deleted-credits [:= :credits.user_id @user-id]))))

  (testing "Retrieve deleted credits based on 'where' conditions: value"
    (credits/add-credit! @user-id 123.45)
    (credits/delete-credit! (:id (first (credits/get-credits [:= :credits.user_id @user-id]))))
    (is (sequential? (credits/get-deleted-credits [:= :credits.value 123.45]))))

  (testing "Retrieve deleted credits based on 'where' conditions: created_at"
    (credits/add-credit! @user-id 453.21)
    (let [created_at (:created_at (first (credits/get-credits [:= :credits.value 453.21])))]
      (is (= 1 (credits/delete-credit! (:id (first (credits/get-credits [:= :credits.created_at created_at]))))))
      (is (sequential? (credits/get-deleted-credits [:= :credits.created_at created_at])))))

  (testing "Retrieve deleted credits with invalid 'where' conditions"
    (is (thrown? Exception
            (credits/get-deleted-credits [:= :credits.value "invalid"])))))

(deftest test-delete-credit!
  (testing "Delete a credit identified by its ID"
    (let [credit-id (:id (first (credits/get-credits [:= :credits.user_id @user-id])))]
      (is (= 1 (credits/delete-credit! credit-id)))))

  (testing "Shouldn't delete credit with invalid ID"
    (is (thrown? Exception
            (credits/delete-credit! "invalid")))))
