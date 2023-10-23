(ns sample.app.models.users
  (:require
    [buddy.hashers :as hashers]
    [clojure.spec.alpha :as spec]
    [clojure.string :as str]
    [sample.app.utils :as utils]))

(def trusted-algs #{:bcrypt+blake2b-512})

(def email-regex
  #"^\w+@[a-zA-Z_]+?\.[a-zA-Z]{2,3}$")

(def password-regex
  #"^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}")

(spec/def :users/uuid
  (spec/and string? #(re-matches utils/uuid-regex %)))

(spec/def :users/email
  (spec/and string? #(re-matches email-regex %)))

(spec/def :users/password
  ;; A password that must contain at least one uppercase letter, one lowercase
  ;; letter, one digit, one special character, and be at least 8 characters long.
  (spec/and string? #(re-matches password-regex %)))

(spec/def :users/create-user!
  (spec/keys :req-un [:users/email :users/password]))

(spec/def :users.password/where
  (spec/keys :req-un [(or :users/email :users/uuid)]))

(spec/def :users/where :general/query)

(spec/def :users/update-users!
  (spec/keys :req-un [:users/where :general.update/set]))

(defn encrypt
  "Encrypts a password using the bcrypt+blake2b-512 algorithm with 13 iterations."
  [password]
  (hashers/derive password {:alg :bcrypt+blake2b-512 :iterations 13}))

(defn check-email
  "Lowercase an email address and validate its format."
  [email]
  (->> email
    (str/lower-case)
    (utils/validate-spec :users/email)))

(defn get-users
  "Retrieves user records based on the 'where' conditions."
  ([]
  (get-users nil))
  ([where]
  (->> where
    (conj [:and [:<> :users.deleted true]])
    (utils/validate-spec :users/where)
    (#(utils/hsql-execute! {:select   [:users.id :users.email
                                       :users.status :users.password
                                       :users.created_at :users.updated_at]
                            :from     [:users]
                            :where    %
                            :order-by [:users.id]})))))

(defn create-user!
  "Creates a new user with the given email and password.
  Returns 1 on success or throws an exception if the user already exists."
  [email password]
  (let [data {:email (str/lower-case email) :password password}]
    (utils/validate-spec :users/create-user! data)
    (if (empty? (get-users [:= :users.email (:email data)]))
      (-> data
        (merge {:password (encrypt password)})
        (#(utils/hsql-execute! {:insert-into [:users]
                                :columns     [:email :password]
                                :values      [[(:email %) (:password %)]]}))
        (utils/format-hsql-output))
      (throw (utils/db-error "User already exists.")))))

(defn get-deleted-users
  "Retrieves deleted user records based on the 'where' conditions."
  ([]
  (get-deleted-users nil))
  ([where]
  (->> where
    (conj [:and [:= :users.deleted true]])
    (utils/validate-spec :users/where)
    (#(utils/hsql-execute! {:select   [:users.id :users.email :users.status
                                       :users.deleted :users.password
                                       :users.created_at :users.updated_at]
                            :from     [:users]
                            :where    %
                            :order-by [:users.id]})))))

(defn update-users!
  "Updates a user's information based on 'where' and 'set' conditions. Returns the number of rows affected"
  [where set]
  (->> {:where [:and [:<> :users.deleted true] where] :set set}
    (utils/validate-spec :users/update-users!)
    (#(utils/hsql-execute! {:update :users
                            :set    (:set %)
                            :where  (:where %)}))
    (utils/format-hsql-output)))

(defn deactivate-user!
  "Deactivates a user by setting their status to 'inactive'. Returns 1 on success"
  [email]
  (-> email
    (check-email)
    (#(update-users! [:= :users.email %] {:status "inactive"}))))

(defn activate-user!
  "Activates a user by setting their status to 'active'. Returns 1 on success"
  [email]
  (-> email
    (check-email)
    (#(update-users! [:= :users.email %] {:status "active"}))))

(defn delete-user!
  "Deletes a user based on their email address. Returns 1 on success"
  [email]
  (-> email
    (check-email)
    (#(utils/hsql-execute! {:update :users
                            :set    {:deleted true}
                            :where  [:and [:<> :deleted true] [:= :email %]]}))
    (utils/format-hsql-output)))

(defn update-password!
  "Updates a user's password with a new one. Returns 1 on success"
  [new-password email]
  (utils/validate-spec :users/password new-password)
  (-> email
    (check-email)
    (#(update-users! [:= :users.email %] {:password (encrypt new-password)}))))

(defn verify-password
  "Verifies a password attempt against the user's stored password hash.
  Returns nil if the user doesn't exist, is inactive, or the password is wrong."
  [attempt email]
  (when-let [user (last (get-users [:= :users.email email]))]
    (when (= (:status user) "active")
      (-> (:password user)
        (#(hashers/verify attempt % {:limit trusted-algs}))
        (:valid)))))
