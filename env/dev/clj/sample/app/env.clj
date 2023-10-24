(ns sample.app.env
  (:require
    [clojure.tools.logging :as log]
    [integrant.repl.state :as state]
    [sample.app.dev-middleware :refer [wrap-dev]]))

#_{:clj-kondo/ignore [:redefined-var]}
(def defaults
  {:init          (fn []
                    (log/info "\n-=[app starting using the development or test profile]=-"))
   :start         (fn []
                    (log/info "\n-=[app started successfully using the development or test profile]=-"))
   :stop          (fn []
                    (log/info "\n-=[app has shut down successfully]=-"))
   :middleware    wrap-dev
   :allow-origin  #"^http(s)?:\/\/(.+\.)?(localhost|127.0.0.1|172.16.238.5)(:\d{4})?$"
   :token-name    "Token"
   :cookie-name   "sample.app.token"
   :opts          {:profile       :dev
                   :persist-data? true}})

#_{:clj-kondo/ignore [:redefined-var]}
(defn environment [] (:system/env state/config))
