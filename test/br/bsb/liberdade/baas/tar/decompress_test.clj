(ns br.bsb.liberdade.baas.tar.decompress-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.tar.decompress :as untar]))

(def good-test-file "./resources/actions.tar.gz")
(def sad-test-file "./resources/pokemon.jpg")

(deftest untar-file--happy-cases
  (testing "It's possible to decompress a tar file and not leave anything behind"
    (let [raw-binary (untar/slurp-bytes good-test-file)
          temp-files-before (untar/list-files untar/tmpd)
          result (untar/extract raw-binary)
          temp-files-after (untar/list-files untar/tmpd)]
      (is (some? raw-binary))
      (is (some? result))
      (is (= 2 (count result)))
      (is (= temp-files-before temp-files-after)))))

(deftest untar-file--sad-cases
  (testing "Decompressing invalid tar files fails gracefully"
    (let [raw-binary (untar/slurp-bytes sad-test-file)
          temp-files-before (untar/list-files untar/tmpd)
          invalid-file-result (untar/extract raw-binary)
          nil-result (untar/extract nil)
          temp-files-after (untar/list-files untar/tmpd)]
      (is (some? raw-binary))
      (is (= temp-files-before temp-files-after))
      (is (nil? invalid-file-result))
      (is (nil? nil-result)))))

