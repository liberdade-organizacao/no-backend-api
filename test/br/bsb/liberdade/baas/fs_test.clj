(ns br.bsb.liberdade.baas.fs-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.fs :as fs]))

(deftest file-crud 
  (testing "can create, read, update, and delete a file"
    (let [filename "test_file.txt"
          _ (fs/delete-file filename)
          contents-read-1 (fs/read-file filename)
          contents "this is a test"
          _ (fs/write-file filename contents)
          contents-read-2 (fs/read-file filename)
          contents-again "this is another test"
          _ (fs/write-file filename contents-again)
          contents-read-3 (fs/read-file filename)
          _ (fs/delete-file filename)
          contents-read-4 (fs/read-file filename)]
      (is (nil? contents-read-1))
      (is (= contents-read-2 contents))
      (is (= contents-read-3 contents-again))
      (is (nil? contents-read-4)))))

