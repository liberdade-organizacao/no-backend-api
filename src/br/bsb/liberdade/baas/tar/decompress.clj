(ns br.bsb.liberdade.baas.tar.decompress
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clj-compress.core :as tar]))

(defn- random-string [length]
  (loop [alphabet "abcdefghijklmnopqrstuvwxyz"
         i 0
         state ""]
    (if (>= i length)
      state
      (recur alphabet
             (inc i)
             (str state (nth alphabet (rand-int (count alphabet))))))))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [in (io/input-stream x)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn spit-bytes [filename contents]
  (with-open [w (io/output-stream filename)]
    (.write w contents)))

(defn list-files [directory]
  (-> directory
      io/file
      file-seq
      rest))

(defn delete-file [file]
  (when (.isDirectory file)
    (run! delete-file (.listFiles file)))
  (io/delete-file file true))

(defn extract [raw-binary]
  (let [temp-id (random-string 7)
        temp-file-name (str "/tmp/" temp-id ".tar.gz")
        temp-output-folder-name (str "/tmp/" temp-id ".d")]
    (try
      (let [_ (spit-bytes temp-file-name raw-binary)
            _ (tar/decompress-archive temp-file-name
                                      temp-output-folder-name
                                      "gz")
            extracted-files (list-files temp-output-folder-name)
            result (->> extracted-files
	                      (map #(.getPath %))
                        (reduce (fn [state file]
                                  (assoc state
                                         (-> file (string/split #"/") last)
                                         (slurp file)))
                                {}))]
        (-> temp-file-name io/file delete-file)
        (-> temp-output-folder-name io/file delete-file)
        result)
    (catch Exception e
      (do
        (-> temp-file-name io/file delete-file)
        (-> temp-output-folder-name io/file delete-file)
        nil)))))

