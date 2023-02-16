(ns br.bsb.liberdade.baas.jobs
  (:require [br.bsb.liberdade.baas.db :as db]
            [br.bsb.liberdade.baas.utils :as utils]))

; ####################
; # TABLE TO RECFILE #
; ####################

(defn- build-rec-header [table-name entries]
  ; TODO list other types
  (str "%rec: " table-name "\n"
       "%key: id"
       "\n"))

(defn- build-rec-row-fx [inlet [k v]]
  (str inlet (name k) ": " (if (= k :contents) (String. v) v) "\n"))

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

