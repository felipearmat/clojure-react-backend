(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [clojure.pprint]
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.repl :as repl]
    ;; [criterium.core :as c] ;; benchmarking
    [expound.alpha :as expound]
    [integrant.core :as ig]
    [integrant.repl :refer [clear go halt prep init reset reset-all]]
    [integrant.repl.state :as state]
    [lambdaisland.classpath.watch-deps :as watch-deps] ;; hot loading for deps
    [migratus.core :as m]
    [sample.app.config :as config]
    #_{:clj-kondo/ignore [:unused-namespace :unused-referred-var]}
    [sample.app.core :refer [start-app]]
    [seeds.seed-dev-data :as seeds]))

;; uncomment to enable hot loading for deps
(watch-deps/start! {:aliases [:dev :test]})

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn dev-prep!
  []
  (integrant.repl/set-prep! (fn []
                              (-> (config/system-config {:profile :dev})
                                  (ig/prep)))))

(defn test-prep!
  []
  (integrant.repl/set-prep! (fn []
                              (-> (config/system-config {:profile :test})
                                  (ig/prep)))))

(repl/set-refresh-dirs "src/clj")

(def refresh repl/refresh)

(defn reset-with
  "Halts current state/system and resets it adding keys on keys-vector"
  [keys-vector]
    (let [current-keys (keys state/system)
          new-keys (distinct (concat keys-vector current-keys))]
      (halt)
      (prep)
      (init new-keys)))

(defn use-system
  "Starts state/system key if it wasn't started and return its value"
  [key]
    (when (not-any? #(= key %) (keys state/system)) (reset-with [key]))
    (get state/system key))

(defn reset-db []
  (m/reset (use-system :db.sql/migrations)))

(defn rollback []
  (m/rollback (use-system :db.sql/migrations)))

(defn migrate []
  (m/migrate (use-system :db.sql/migrations)))

(defn seeds []
  (seeds/seeds))

(defn create-migration
  [migration-name]
  (m/create (use-system :db.sql/migrations) migration-name))

(defn start-repl
  ([_]
    (dev-prep!)
    (prep)
    (use-system :db.sql/connection))
  ([] (start-repl nil)))

(defn start-dev [_]
  (dev-prep!)
  (go))

(comment
  (prep) ;; this sets state/config
  (init) ;; this sets state/system
  (go) ;; (go) is a sugar syntax for (do (prep)(init))
  (halt)
  (reset)
  (clear) ;; this unsets state/config
  (reset-all)) ;; Reset-all does halt, clear, prep and init
