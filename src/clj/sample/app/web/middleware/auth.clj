(ns sample.app.web.middleware.auth
  (:require
    [buddy.auth :refer [authenticated?]]
    [reitit.middleware :as middleware]
    [ring.util.http-response :refer [unauthorized]]
    [sample.app.auth.core :as auth]))

(defn wrap-token-from-cookie
  [handler]
  (fn [request]
    (cond-> request
      (auth/no-authorization? request) (auth/set-token-from-cookie)
      true (handler))))

(defn wrap-authentication
  [handler]
  (fn [request]
    (if-not (authenticated? request)
      (unauthorized)
      (handler request))))

(defn wrap-renew-token
  [handler]
  (fn [request]
    (if (and (authenticated? request) (not= "/api/auth" (:uri request)))
      (let [new-cookie (auth/generate-cookie (:user (:identity request)))]
        (->> request
          (handler)
          (merge {:cookies new-cookie})))
      (handler request))))

(def authentication-middleware
  (middleware/map->Middleware
    {:name ::authentication-middleware
     :description "Middleware that checks authentication and authorization"
     :wrap wrap-authentication}))

(def renew-token-middleware
  (middleware/map->Middleware
    {:name ::renew-token-middleware
     :description "Middleware that renew authorization token after each call"
     :wrap wrap-renew-token}))
