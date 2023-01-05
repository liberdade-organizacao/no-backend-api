(ns br.bsb.liberdade.baas.utils
  (:require [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]))

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
