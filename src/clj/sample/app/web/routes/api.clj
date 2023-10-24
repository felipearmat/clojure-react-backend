(ns sample.app.web.routes.api
  (:require
    [integrant.core :as ig]
    [reitit.coercion.malli :as malli]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.swagger :as swagger]
    [sample.app.env :refer [environment]]
    [sample.app.web.controllers.auth :as auth]
    [sample.app.web.controllers.calculator :as calculator]
    [sample.app.web.controllers.health :as health]
    [sample.app.web.controllers.record :as record]
    [sample.app.web.middleware.auth :as middleware]
    [sample.app.web.middleware.exception :as exception]
    [sample.app.web.middleware.formats :as formats]))

(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance
   :swagger    {:id ::api}
   :middleware [middleware/renew-token-middleware
                ;; query-params & form-params
                parameters/parameters-middleware
                  ;; content-negotiation
                muuntaja/format-negotiate-middleware
                  ;; encoding response body
                muuntaja/format-response-middleware
                  ;; exception handling
                coercion/coerce-exceptions-middleware
                  ;; decoding request body
                muuntaja/format-request-middleware
                  ;; coercing response bodys
                coercion/coerce-response-middleware
                  ;; coercing request parameters
                coercion/coerce-request-middleware
                  ;; exception handling
                exception/wrap-exception]})

;; Routes
(defn api-routes [_opts]
  [["/swagger.json" {:get {:no-doc  true
                     :swagger {:info {:title "sample.app API"}}
                     :handler (swagger/create-swagger-handler)}}]
    ["/health" {:get health/healthcheck!}]
    ["/login"  {:post auth/login!}]
    ["/logout" {:post auth/logout!}]
    ["/data"   {:get auth/data}]
    ["/v1"     {:middleware [middleware/authentication-middleware]}
      ["/records"        {:get record/get-records}]
      ["/record/delete" {:post record/delete-records!}]
      ["/calculate"     {:post calculator/calculate}]]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path route-data (api-routes opts)])
