(ns sample.app.calculator.core
  (:require
    [clj-http.client :as client]
    [clojure.spec.alpha :as spec]
    [clojure.string :as str]
    [infix.macros :as inf]
    [sample.app.env :refer [environment]]
    [sample.app.models.credits :as credits]
    [sample.app.models.operations :as operations]
    [sample.app.models.records :as records]
    [sample.app.models.users :as users]
    [sample.app.utils :as utils]))

;; Define the expression spec for validation
(spec/def :calculator/expression
  (spec/and string? #(re-matches #"(?:[0-9√()+\-*\/.]+|randomstr)" %)))

;; Define the URL for fetching random strings
(def random-org-url
  "https://www.random.org/strings/?num=1&len=32&digits=on&upperalpha=on&loweralpha=on&unique=on&format=plain&rnd=new")

(defn char-occur
  "Count the occurrences of a specific character in a string."
  [str char]
  (count (filter #(= char %) str)))

(defn gen-random-string
  "Generate a random string using an external API."
  []
  (str/replace (:body (client/get random-org-url)) #"\n" ""))

(defn count-operators
  "Count occurrences of mathematical operators in an expression."
  [expression]
  (let [map-names {:addition       \+
                   :subtraction    \-
                   :multiplication \*
                   :division       \/
                   :square_root    \√}]
    (zipmap (keys map-names)
            (map #(char-occur expression %) (vals map-names)))))

(defn get-user-balance
  "Get a user balance. Return it's value with centesimal precision"
  [email]
  (let [credits (credits/get-credits [:= :users.email email])
        records (records/get-records [:= :users.email email])]
    (read-string (format "%.2f"
      (- (reduce + (map :value credits)) (reduce + (map :amount records)))))))

(defn record-operation
  "Create a record for a specific operation."
  ([user-id operation operation-response]
    (records/create-record! {:operation_id (:id operation)
                             :user_id      user-id
                             :amount       (:cost operation)
                             :operation_response operation-response}))
  ([user-id operation]
    (record-operation user-id operation "")))

(defn eval-expression
  "Evaluate an infixed mathematical expression in string format."
  [expression]
  ((inf/from-string [] (str/replace expression #"√" "sqrt"))))

(defmulti process-expression
  "Handle general mathematical expressions by evaluating the expression, recording
  operations based on the operator counts, and returning the evaluation result."
  (fn [_ expression] expression))

(defmethod process-expression "randomstr" [user-id _]
  (let [randomstr-op (first (operations/get-operations [:= :type "random_string"]))
        my-user (first (users/get-users [:= :id user-id]))
        balance (get-user-balance (:email my-user))
        random-string (gen-random-string)]
    (if (> (:cost randomstr-op) balance)
      {:msg "Insufficient balance to calculate Expression."}
      (do
        (record-operation user-id randomstr-op random-string)
        random-string))))

(defmethod process-expression :default [user-id expression]
  (let [ops (operations/get-operations)
        my-user (first (users/get-users [:= :id user-id]))
        times-map (count-operators expression)
        total-cost (reduce + (map #(* (get times-map (keyword (:type %)) 0) (:cost %)) ops))
        balance (get-user-balance (:email my-user))
        result (eval-expression expression)]
    (if (> total-cost balance)
      {:msg "Insufficient balance to calculate Expression."}
      (do
        (doseq [op ops]
          (when-let [times ((keyword (:type op)) times-map)]
            (dotimes [_ times]
              (record-operation user-id op result))))
        result))))

(defn expand-expression-operators
  "Expand '√' to '*√' when preceded by a digit or ')' to ensure correct evaluation."
  [expression]
  (str/replace expression #"(?<=\d|\))√" "*√"))

(defn calculate!
  "Validate, calculate, and charge for a mathematical expression."
  [user-id expression]
  (try
    (->> expression
      (utils/validate-spec :calculator/expression)
      (expand-expression-operators)
      (process-expression user-id))
  (catch Exception e
    (when (= :dev (environment))
      (throw e))
    {:msg "Invalid Expression."})))
