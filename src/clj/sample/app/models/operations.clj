(ns sample.app.models.operations
  (:require
    [clojure.spec.alpha :as spec]
    [sample.app.utils :as utils]))

(spec/def :operations/id int?)

(spec/def :operations/type
  #{"addition"
    "subtraction"
    "multiplication"
    "division"
    "square_root"
    "random_string"})

(spec/def :operations/cost number?)

(spec/def :operations/create-operation!
  (spec/keys :req-un [:operations/cost :operations/type]))

(spec/def :operations/delete-operation!
  (spec/keys :req-un [:operations/cost :operations/type]))

(spec/def :operations/where :general/query)

(spec/def :operations/update-operations!
  (spec/keys :req-un [:operations/where :general.update/set]))

(defn create-operation!
  "Creates a new operation with the given type and cost.
  Returns 1 on success."
  [type cost]
  (->> {:type type :cost cost}
    (utils/validate-spec :operations/create-operation!)
    (#(utils/hsql-execute! {:insert-into [:operations]
                            :columns     [:type :cost]
                            :values      [[(:type %) (:cost %)]]}))
    (utils/format-hsql-output)))

(defn get-operations
  "Retrieves operations based on the 'where' conditions.
  Returns a collection of maps containing operation fields."
  ([]
  (get-operations nil))
  ([where]
  (->> where
    (conj [:and [:<> :operations.deleted true]])
    (utils/validate-spec :operations/where)
    (#(utils/hsql-execute! {:select   [:operations.id :operations.type
                                       :operations.cost :operations.created_at
                                       :operations.updated_at]
                            :from     [:operations]
                            :where    %
                            :order-by [:operations.id]})))))

(defn get-deleted-operations
  "Retrieves operations based on the 'where' conditions.
  Returns a collection of maps containing operation fields."
  ([]
  (get-deleted-operations nil))
  ([where]
  (->> where
    (conj [:and [:= :operations.deleted true]])
    (utils/validate-spec :operations/where)
    (#(utils/hsql-execute! {:select   [:operations.id :operations.type
                                       :operations.cost :operations.created_at
                                       :operations.updated_at :operations.deleted]
                            :from     [:operations]
                            :where    %
                            :order-by [:operations.id]})))))

(defn delete-operation!
  "Deletes an operation identified by its unique ID.
  Returns 1 on success."
  [id]
  (->> id
    (utils/validate-spec :operations/id)
    (#(utils/hsql-execute! {:update :operations
                            :set    {:deleted true}
                            :where  [:and [:<> :deleted true] [:= :id %]]}))
    (utils/format-hsql-output)))

(defn update-operations!
  "Updates operations based on 'where' and 'set' conditions.
  Returns the number of affected rows."
  [where set]
  (->> {:where [:and [:<> :operations.deleted true] where] :set set}
    (utils/validate-spec :operations/update-operations!)
    (#(utils/hsql-execute! {:update :operations
                            :set    (:set %)
                            :where  (:where %)}))
    (utils/format-hsql-output)))
