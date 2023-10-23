(ns seeds.seed-dev-data
  (:require
    [sample.app.models.credits :as credits]
    [sample.app.models.operations :as operations]
    [sample.app.models.records :as records]
    [sample.app.models.users :as users]))

(def users-data [
  {:status "active"   :deleted false :email "admin@sample.com"}
  {:status "inactive" :deleted false :email "deactivated@sample.com"}
  {:status "active"   :deleted true  :email "deleted@sample.com"}
  {:status "active"   :deleted false :email "test@sample.com"}
  {:status "active"   :deleted false :email "user@sample.com"}
])

(def operations-data [
  {:cost 1.00 :deleted false :type "addition"}
  {:cost 1.00 :deleted false :type "subtraction"}
  {:cost 1.50 :deleted false :type "multiplication"}
  {:cost 2.00 :deleted false :type "division"}
  {:cost 3.00 :deleted false :type "square_root"}
  {:cost 4.00 :deleted true  :type "random_string"}
  {:cost 3.50 :deleted false :type "random_string"}
])

(defn gen-records [num-records]
  (let [users (users/get-users)
        ops   (operations/get-operations)]
    (doseq [i (range num-records)]
      (let [op       (rand-nth ops)
            user     (rand-nth users)
            response (str "Response " i)]
        (records/create-record!
          {:operation_id       (:id op)
           :user_id            (:id user)
           :amount             (:cost op)
           :operation_response response})))))

(defn seeds []
  (doseq [user users-data]
    (users/create-user! (:email user) "Password@1")
    (when (:deleted user)
      (users/delete-user! (:email user))))
  (doseq [user (users/get-users)]
    (when-not (:deleted user)
      (credits/add-credit! (:id user) 1000)))
  (doseq [operation operations-data]
    (operations/create-operation! (:type operation) (:cost operation))
    (when (:deleted operation)
      (operations/delete-operation! (:id (first (operations/get-operations [:= :type (:type operation)]))))))
  (gen-records 200)
  (println "Seeds completed!"))
