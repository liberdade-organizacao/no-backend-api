(ns br.bsb.liberdade.baas.fs
  (:require [clojure.java.io :as io]))

(def fs-folder "./db/fs")

(defn- filepath [filename]
  (str fs-folder "/" filename))

(defn write-file [filename contents]
  (io/make-parents (filepath filename))
  (spit (filepath filename) contents))

(defn read-file [filename]
  (try
    (slurp (filepath filename))
    (catch Exception ex
      nil)))

(defn delete-file [filename]
  (try
    (io/delete-file (filepath filename))
    (catch Exception ex
      nil)))

