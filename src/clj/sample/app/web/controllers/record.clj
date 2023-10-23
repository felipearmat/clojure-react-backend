(ns sample.app.web.controllers.record
  (:require
    [ring.util.http-response :as http-response]
    [sample.app.calculator.core :as calculator]
    [clojure.tools.logging :as log]
    [sample.app.models.records :as records]))

(defn get-records
  [req]
  (let [user (:user (:identity req))
        params (:params req)
        operation-type (:operationType params)
        amount-operator (:amountOperator params)
        amount-value (:amountValue params)
        query [:and
                [:= :users.email user]
                (when (seq operation-type)
                  [:= :operations.type operation-type])
                (when (and (seq amount-operator) (seq amount-value))
                  [(keyword amount-operator) :amount (Float/parseFloat amount-value)])]]
        (http-response/ok {:records (records/get-records query)})))

(defn delete-records!
  [{:keys [identity body-params]}]
  (let [email (:user identity)
        records (:records body-params)
        total (records/delete-records! records)
        user-balance (calculator/get-user-balance email)
        message (str " record" (when (> total 1) "s") " deleted sucessfully!")]
        (http-response/ok {:message (str total message)
                           :balance user-balance})))
