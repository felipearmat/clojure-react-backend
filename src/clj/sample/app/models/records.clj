(ns sample.app.models.records
  (:require
    [clojure.spec.alpha :as spec]
    [sample.app.utils :as utils]))

(spec/def :records/id int?)
(spec/def :records/operation_id int?)
(spec/def :records/user_id :users/uuid)
(spec/def :records/amount #(and (number? %) (pos? %)))
(spec/def :records/operation-response string?)
(spec/def :records/deleted boolean?)
(spec/def :records/operation_response string?)
(spec/def :records/delete-records! (spec/coll-of :records/id :distinct true :into []))

(spec/def :records/create-record!
  (spec/keys :req-un [:records/operation_id
                      :records/user_id
                      :records/amount]))

(def select-fields
  [[:operations.cost :operation_cost]
   [:operations.type :operation_type]
   [:records.amount :amount]
   [:records.created_at :created_at]
   [:records.user_id :user_id]
   [:records.operation_id :operation_id]
   [:records.id :id]
   [:records.operation_response :operation_response]
   [:records.updated_at :updated_at]
   [:users.email :user_email]
   [:users.status :user_status]])


(defn execute-insert
  "Formats data into a Honeysql query and execute it"
  [data]
  (let [columns (vec (keys data))
        values  (mapv #(get data %) columns)]
        (utils/hsql-execute! {:insert-into [:records]
                              :columns     columns
                              :values      [values]})))

(defn create-record!
  "Create a new record with the provided data. Returns 1 on success."
  [record-data]
  (->> record-data
    (#(merge {:operation_response "No response."} %))
    (utils/validate-spec :records/create-record!)
    (execute-insert)
    (utils/format-hsql-output)))

(defn get-records
  "Retrieve records based on the 'where' conditions. Returns a coll of retrieved records."
  ([]
  (get-records nil))
  ([where]
  (->> where
    (conj [:and [:<> :records.deleted true]])
    (utils/validate-spec :general/query)
    (#(utils/hsql-execute! {:select    select-fields
                            :from      [:records]
                            :join      [:operations [:= :records.operation_id :operations.id]
                                        :users      [:= :records.user_id :users.id]]
                            :where      %
                            :order-by  [[:records.id :desc]]})))))

(defn get-deleted-records
  "Retrieve deleted records based on the 'where' conditions. Returns a coll of retrieved records."
  ([]
  (get-deleted-records nil))
  ([where]
  (->> where
    (#(vec (conj [:and [:= :records.deleted true]] %)))
    (utils/validate-spec :general/query)
    (#(utils/hsql-execute! {:select   (conj select-fields [:records.deleted :deleted])
                            :from      :records
                            :join     [:operations [:=
                                                     :records.operation_id
                                                     :operations.id]
                                       :users      [:=
                                                     :records.user_id
                                                     :users.id]]
                            :order-by [[:records.id :desc]]
                            :where    %})))))

(defn delete-record!
  "Delete a record identified by its ID. Returns 1 on success."
  [id]
  (->> id
    (utils/validate-spec :records/id)
    (#(utils/hsql-execute! {:update :records
                            :set    {:deleted true}
                            :where  [:and [:<> :deleted true] [:= :id %]]}))
    (utils/format-hsql-output)))

(defn delete-records!
  "Delete records identified by a vector of its ID. Returns number of columns deleted."
  [ids]
  (->> ids
    (utils/validate-spec :records/delete-records!)
    (#(utils/hsql-execute! {:update :records
                            :set    {:deleted true}
                            :where  [:and [:<> :deleted true] [:in :id %]]}))
    (utils/format-hsql-output)))
