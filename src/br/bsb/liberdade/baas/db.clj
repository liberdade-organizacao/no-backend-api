(ns br.bsb.liberdade.baas.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.java.io :as io]
            [br.bsb.liberdade.strint :as strint]
            [br.bsb.liberdade.baas.utils :as utils]))

(def sql-resources-folder "./resources/sql")
(def sql-migrations-folder (str sql-resources-folder "/migrations"))
(def sql-operations-folder (str sql-resources-folder "/operations"))
(def sql-migrations (utils/read-sql-dir sql-migrations-folder))
(def sql-operations (utils/read-sql-dir sql-operations-folder))
(def db (or (System/getenv "JDBC_DATABASE_URL")
            "jdbc:postgresql://localhost:5434/baas?user=liberdade&password=password"))
(def ds (jdbc/get-datasource db))
(Class/forName "org.postgresql.Driver")  ; required to get the driver working properly

(defn execute-query [query]
  (jdbc/execute! ds [query] {:builder-fn rs/as-unqualified-lower-maps}))

; ##############
; # MIGRATE UP #
; ##############

(defn- check-if-migration-exists [migration]
  (->> {"migration" migration}
       (strint/strint (get sql-operations "check-if-migration-exists.sql"))
       execute-query
       first
       :count
       pos?))

(defn- run-migration [migration-file-name]
  (->> migration-file-name
       (get sql-migrations)
       execute-query))

(defn- add-migration [migration-id]
  (->> {"migration" migration-id}
       (strint/strint (get sql-operations "add-migration.sql"))
       execute-query))

(defn- maybe-run-migration [migration-file-name]
  (let [migration-id (-> migration-file-name
                         (clojure.string/split #"\.")
                         first)]
    (when (not (check-if-migration-exists migration-id))
      (run-migration migration-file-name)
      (add-migration migration-id))))

(defn run-migrations []
  (let [files (->> sql-migrations-folder
                   utils/list-dir
                   (filter #(re-find #"(.*?)\.up\.sql$" %))
                   sort)
        limit (count files)]
    (loop [n 0]
      (when (< n limit)
        (maybe-run-migration (nth files n))
        (recur (inc n))))))

; ################
; # MIGRATE DOWN #
; ################

(defn- get-last-migration []
  (-> sql-operations
      (get "get-last-migration.sql")
      execute-query
      first
      (get :name)))

(defn undo-last-migration []
  (let [last-migration (get-last-migration)]
    (execute-query (get sql-migrations (str last-migration ".down.sql")))
    (execute-query (get sql-operations "remove-last-migration.sql"))))

(defn undo-migrations []
  (let [files (->> sql-migrations-folder
                   utils/list-dir
                   (filter #(re-find #"(.*?)\.down\.sql$" %))
                   sort
                   reverse)
        limit (count files)]
    (loop [n 0]
      (when (< n limit)
        (execute-query (get sql-migrations (nth files n)))
        (execute-query (get sql-operations "remove-last-migration.sql"))
        (recur (inc n))))))

(defn check-health []
  (try
    (let [result (-> sql-operations
                     (get "check-health.sql")
                     execute-query)
          ok (some? result)]
      (if ok
        "ok"
        "ko"))
    (catch Exception ex
      "ko")))
(defn- get-last-migration []
  (-> sql-operations
      (get "get-last-migration.sql")
      execute-query
      first
      (get :name)))


; #####################
; # GLOBAL OPERATIONS #
; #####################

(defn drop-database []
  (undo-migrations))

(defn setup-database []
  (jdbc/execute! ds [(get sql-operations "setup-database.sql")]))

(defn run-operation [operation params]
  (let [raw-sql (get sql-operations operation)
        query (strint/strint raw-sql params)
        result (execute-query query)]
    result))

(defn run-operation-first [operation params]
  (first (run-operation operation params)))

