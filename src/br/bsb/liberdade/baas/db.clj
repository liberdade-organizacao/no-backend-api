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
(def dbname "./db/database.sqlite")
(def ds (jdbc/get-datasource {:dbtype "sqlite"
                              :dbname dbname}))

(defn execute-query [query]
  (with-open [connection (jdbc/get-connection ds)]
    (jdbc/execute! connection
                   ["PRAGMA foreign_keys = ON;"])
    (jdbc/execute! connection
                   [query]
                   {:builder-fn rs/as-unqualified-lower-maps})))

; ##############
; # MIGRATE UP #
; ##############

(defn- check-if-migration-exists [migration]
  (-> (strint/strint (get sql-operations "check-if-migration-exists.sql")
                     {"migration" migration})
      execute-query
      first
      (get (keyword "count(*)"))
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

(defn undo-migration [migration]
  (execute-query (get sql-migrations (str migration ".down.sql")))
  (execute-query (strint/strint (get sql-operations "remove-migration.sql")
                                {"migration" migration})))

(defn undo-last-migration []
  (-> (get-last-migration) undo-migration))

(defn undo-migrations []
  (->> (keys sql-migrations)
       (filter #(re-find #"(.*?)\.down\.sql$" %))
       (map #(first (clojure.string/split % #"\.")))
       sort
       reverse
       (mapv undo-migration)))

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

