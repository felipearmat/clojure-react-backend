(ns sample.app.web.middleware.core
  (:require
    [buddy.auth.backends.token :refer [jwe-backend]]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [clojure.tools.logging :as log]
    [iapetos.collector.ring :as prometheus-ring]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.defaults :as defaults]
    [ring.middleware.session.cookie :as cookie]
    [sample.app.config :refer [secret-key]]
    [sample.app.env :as env]
    [sample.app.web.middleware.auth :refer [wrap-token-from-cookie]]))

;; Create jwe based backend manager
(def auth-backend (jwe-backend {:secret secret-key
                                :options {:alg :a256kw :enc :a128gcm}
                                :token-name (:token-name env/defaults)}))

(defn log-request
  [request & vars]
  (when (= :dev (env/environment))
    (log/info "\n\n" (apply str vars) request "\n\n"))
  request)

(defn wrap-logger
  [handler]
  (fn [request]
    (-> request
      (log-request "Logging Request:")
      (handler)
      (log-request "Logging Response:"))))

;; Get the allowed regex for CORS by ENV
(def allowed-cors-regex
  (:allow-origin env/defaults))

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret] :as opts}]
  (let [cookie-store (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})]
    (fn [handler]
      (-> ((:middleware env/defaults) handler opts)
          (wrap-logger)
          (wrap-authorization auth-backend)
          (wrap-authentication auth-backend)
          (wrap-token-from-cookie)
          ;; Should put anything that uses session above wrap-defaults
          (defaults/wrap-defaults
            (assoc-in site-defaults-config [:session :store] cookie-store))
          (wrap-cors
            :access-control-allow-origin  [allowed-cors-regex]
            :access-control-allow-methods [:get :put :post :delete])
          (cond->
            (some? metrics) (prometheus-ring/wrap-metrics metrics))))))
