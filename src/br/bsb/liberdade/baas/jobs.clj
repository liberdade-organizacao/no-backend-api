(ns br.bsb.liberdade.baas.jobs
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.utils :as utils]))

; ####################
; # TABLE TO RECFILE #
; ####################

(defn- build-rec-header [table-name entries]
  ; TODO list other types
  (str "%rec: " table-name "\n"
       "%key: id\n"
       "\n"))

(defn- stringify [k v]
  (cond
    (= k :contents)
    (String. v)
    (= k :script)
    (json/write-str v)
    :else
    v))

(defn- build-rec-row-fx [inlet [k v]]
  (str inlet (name k) ": " (stringify k v) "\n"))

(defn- build-rec-entry-fx [outlet entry]
  (str outlet
       (reduce build-rec-row-fx
               ""
               entry)
       "\n"))

(defn- edn-to-rec [table-name entries]
  (reduce build-rec-entry-fx
          (build-rec-header table-name entries)
          entries))

(defn to-recfile [table-name output-file]
  (->> (str "SELECT * FROM " table-name ";")
       db/execute-query
       (edn-to-rec table-name)
       (spit output-file)))

; ####################
; # RECFILE TO TABLE #
; ####################

(defn- rec-to-edn [inlet]
  (->> (string/split inlet #"\n\n")
       (map #(string/split % #"\n"))
       (map (fn [fields]
              (reduce (fn [state field]
                        (let [matches (re-find #"(.*)\: (.*)" field)]
                          (assoc state (nth matches 1) (nth matches 2))))
                      {}
                      fields)))
       rest))

(defn- destringify [k v]
  (cond
    (= k "script")
    (json/read-str v)
    :else
    v))

(defn- filter-empty-vals [recs]
  (filter (fn [[k v]]
            (-> v empty? not))
          recs))

(defn- upsert-rec [table-name rec]
  (let [vars (->> rec
                  filter-empty-vals
                  keys
                  (string/join ","))
        values (->> rec
                    filter-empty-vals
                    (map (fn [[k v]]
                           (destringify k v)))
                    (map #(str "'" % "'"))
                    (string/join ","))
        rec-id (get rec "id")
        vars-values (->> rec
                         filter-empty-vals
                         (map (fn [[k v]]
                                (str k "='" (destringify k v) "'")))
                         (string/join ","))
        query (str "INSERT INTO " table-name
                   "(" vars ") "
                   "VALUES(" values ") "
                   "ON CONFLICT (id) DO "
                   "UPDATE SET " vars-values " "
                   "RETURNING *;")]
    (db/execute-query query)))

(defn- upsert-recs [table-name recs]
  (reduce (fn [state rec]
            (conj state (upsert-rec table-name rec)))
          []
          recs))

(defn from-recfile [table-name input-file]
  (->> input-file
       slurp
       rec-to-edn
       (upsert-recs table-name)))

