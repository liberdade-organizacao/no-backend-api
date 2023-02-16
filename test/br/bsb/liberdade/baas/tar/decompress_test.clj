(ns br.bsb.liberdade.baas.tar.decompress-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.tar.decompress :as untar]))

(def good-test-file "./resources/actions.tar.gz")

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [in (clojure.java.io/input-stream x)
              out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy in out)
    (.toByteArray out)))

(deftest untar-file--happy-cases
  (testing "It's possible to decompress a tar file and not leave anything behind"
    (let [raw-binary (slurp-bytes good-test-file)
          result (untar/extract raw-binary)]
      (println result)
      (is (some? raw-binary))
      (is (some? result))
      ; TODO check if temporary files were deleted
      )))

