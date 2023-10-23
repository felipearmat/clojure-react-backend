(ns sample.app.web.controllers.calculator
  (:require
    [ring.util.http-response :as http-response]
    [sample.app.calculator.core :as calculator]
    [sample.app.models.users :as users]))

(defn calculate
  [{:keys [identity body-params]}]
  (let [email (:user identity)
        user (first (users/get-users [:= :users.email email]))
        expression (:expression body-params)
        result (calculator/calculate! (:id user) expression)
        user-balance (calculator/get-user-balance email)]
    (if (:msg result)
      (http-response/bad-request (:msg result))
      (http-response/ok {:result result :balance user-balance}))))
