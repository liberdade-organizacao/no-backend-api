(ns br.bsb.liberdade.baas.utils
  (:require [clj-branca.core :as branca]
            [clojure.edn :as edn]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs])
  (:import java.util.Base64))

(def salt (or (System/getenv "SALT") "supersecretkeyyoushouldnotcommit"))

(defn encode-secret [data]
  (->> data
       str
       (branca/encode salt)))

(defn decode-secret [secret]
  (try
    (->> secret
         (branca/decode salt)
         (String.)
         edn/read-string)
    (catch clojure.lang.ExceptionInfo e nil)))

(defn hide [secret]
  (-> (hash/md5 (str secret salt))
      (codecs/bytes->hex)))

(defn encode-data [data]
  (.encodeToString (Base64/getEncoder) (.getBytes data)))

(defn decode-data [data]
  (String. (.decode (Base64/getDecoder) data)))

(defn list-dir [dir]
  (let [directory (clojure.java.io/file dir)
        files (file-seq directory)]
    (map #(.getName %) files)))

(defn read-sql-dir [dir]
  (->> dir
       list-dir
       (filter #(re-find #"(.*?)\.sql$" %))
       (reduce (fn [state file]
                 (assoc state
                        file
                        (slurp (str dir "/" file))))
               {})))

(defn in? [coll it]
  (some #(= % it) coll))

(defn spy [it]
  (println it)
  it)

