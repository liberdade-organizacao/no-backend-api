(ns br.bsb.liberdade.baas.tar.decompress-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.tar.decompress :as untar]))

(def good-test-file "./resources/actions.tar.gz")

(deftest untar-file--happy-cases
  (testing "It's possible to decompress a tar file and not leave anything behind"
    (let [raw-binary (untar/slurp-bytes good-test-file)
          temp-files-before (untar/list-files "/tmp")
          result (untar/extract raw-binary)
	  temp-files-after (untar/list-files "/tmp")]
      (is (some? raw-binary))
      (is (some? result))
      (is (= 2 (count result)))
      (is (= temp-files-before temp-files-after)))))

