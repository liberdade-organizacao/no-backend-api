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

(defn- table-to-edn [table-name]
  (->> (str "SELECT * FROM " table-name ";")
       db/execute-query
       (edn-to-rec table-name)))

(defn to-recfile [output-file & [tables]]
  (let [table-names (if (nil? tables)
                      (db/get-all-tables)
                      tables)]
    (->> (map table-to-edn table-names)
         (reduce str "")
         (spit output-file))))

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
  (doseq [rec recs]
    (upsert-rec table-name rec)))

(defn- is-header? [line]
  (re-find #"%rec:" line))

(defn- get-table-name [recs]
  (-> (string/split-lines recs)
      first
      (string/split #": ")
      second))

(defn- lines-to-recs-by-table-name [lines]
  (loop [head (first lines)
         tail (rest lines)
         rec nil
         outlet {}]
    (if (nil? head)
      (assoc outlet
             (get-table-name rec)
             rec)
      (let [header? (is-header? head)
            first-line? (nil? rec)]
        (recur (first tail)
               (rest tail)
               (cond
                 first-line? head
                 header? head
                 :else (str rec "\n" head))
               (if (and header? (not first-line?))
                 (assoc outlet
                        (get-table-name rec)
                        rec)
                 outlet))))))

(defn from-recfile [input-file]
  (let [recs-by-table-name (-> (slurp input-file)
                               string/split-lines
                               lines-to-recs-by-table-name)]
    (doseq [[table-name raw-recs] recs-by-table-name]
      (upsert-recs table-name
                   (rec-to-edn raw-recs)))))

