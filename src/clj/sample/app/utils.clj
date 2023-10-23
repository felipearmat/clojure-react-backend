(ns sample.app.utils
  (:require
    [clojure.spec.alpha :as spec]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [honey.sql :as hsql]
    [integrant.repl.state :as state]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [sample.app.env :refer [environment]]))

(def uuid-regex
  #"^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$")

(spec/def :general/where #(or (map? %) (= nil %)))

(spec/def :general/query #(and (vector? %) (not= % [])))

(spec/def :general/set map?)

(defn contains-some?
  "Checks if any element in coll2 is present in coll1."
  [coll1 coll2]
  (some (set coll1) coll2))

(spec/def :general/unpermitted-set
  #(not (contains-some? % [:id, :updated_at, :created_at])))

(spec/def :general.update/set
  (spec/merge :general/set
              :general/unpermitted-set))

(defn spec-error
  "Generates an exception with a spec validation error message."
  [spec input]
  (ex-info (spec/explain-str spec input)
           {:type :system.exception/business}))

(defn db-error
  "Generates an exception with a database error message."
  [error]
  (ex-info (or (ex-message error) error)
           (merge (ex-data error)
                  {:type :system.exception/business})))

(defn validate-spec
   "Validates input against a spec. Returns
   input if valid, otherwise throws an exception."
  [spec input]
  (if (spec/valid? spec input)
    input
    (throw (spec-error spec input))))

(defn format-hsql-output
  "Formats data of returning Honeysql execute query to match hugsql format"
  [response]
  (:next.jdbc/update-count (first response)))

(defn db-connector
  "Retrieves the database query function from the system state."
  []
  (if-let [data-source (:db.sql/connection state/system)]
    data-source
    (throw
      (ex-info
        (str
          "Database connection not initialized. Did you execute"
          " (integrant.repl/prep) and (integrant.repl/init)?")
        {:type :system.exception/db-connector-failure}))))

(defn conn-hsql-execute!
  [conn sqlmap]
    (when (= :dev (environment))
      (log/info (hsql/format sqlmap)))
    (jdbc/execute! conn (hsql/format sqlmap) {:builder-fn rs/as-unqualified-maps}))

(defn hsql-execute!
  [sqlmap]
    (conn-hsql-execute! (db-connector) sqlmap))

(defn extract-ns
  [field]
  (when (str/includes? field "->")
    (str (first (str/split field #"->")) ".")))

(defn map-in? [map1 map2]
  (every? (fn [[k v]] (= (get map2 k) v)) map1))
