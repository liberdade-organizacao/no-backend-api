(ns br.bsb.liberdade.baas.tar.decompress
  (:require [clojure.java.io :as io]
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

(defn extract [raw-binary]
  (let [temp-id (random-string 7)
        temp-file-name (str "/tmp/" temp-id ".tar.gz")
	temp-output-folder-name (str "/tmp/" temp-id ".d")
	_ (spit temp-file-name raw-binary)
	result (tar/decompress-archive temp-file-name temp-output-folder-name "gz")]
    ; TODO read files from temp output folder name
    ; TODO delete temporary files
    result))
 
