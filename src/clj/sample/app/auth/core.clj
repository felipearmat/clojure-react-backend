(ns sample.app.auth.core
  (:require
    [buddy.sign.jwt :as jwt]
    [clj-time.core :as time]
    [clojure.string :as str]
    #_{:clj-kondo/ignore [:unresolved-var]}
    [ring.util.http-response :refer [get-header]]
    [sample.app.config :refer [secret-key]]
    [sample.app.env :as env]))

(defn generate-cookie
  [identificator]
  (let [claims      {:user (keyword identificator)
                     :exp  (time/plus (time/now) (time/minutes 15))}
        token       (jwt/encrypt claims secret-key {:alg :a256kw :enc :a128gcm})
        token-name  (str (:token-name env/defaults) " ")
        cookie-name (:cookie-name env/defaults)]
    {cookie-name {:expires   (:exp claims)
                  :http-only true
                  :path      "/"
                  :same-site :strict
                  :secure    (= (env/environment) :prod)
                  :value     (str token-name token)}}))

(defn set-authorization-token
  [token request]
  (assoc-in request [:headers "authorization"] token))

(defn get-token-from-cookie
  [request]
  (get-in request [:cookies (:cookie-name env/defaults) :value]))

(defn set-token-from-cookie
  [request]
  (-> request
    (get-token-from-cookie)
    (set-authorization-token request)))

(defn no-authorization?
  [request]
  (let [header (get-header request "authorization")]
    (empty?
      (str/trim
        (str/replace (str header) (re-pattern (:token-name env/defaults)) "")))))
