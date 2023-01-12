(ns br.bsb.liberdade.baas.utils
  (:require [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs])
  (:import java.util.Base64))

(def salt (-> (hash/md5 (or (System/getenv "SALT") "SALT")) (codecs/bytes->hex)))

(defn encode-secret [data]
  (jwt/sign data salt))

(defn decode-secret [secret]
  (try
    (jwt/unsign secret salt)
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

