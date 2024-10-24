(ns br.bsb.liberdade.baas.utils-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.utils :as utils]))

(def random-token
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MTIzNH0.rWp8vvb4aDZAGcHEYjhCe9qaaf8mSyvyLeyC1QuZWU0")

(deftest token-handling
  (testing "can encode and decode data"
    (let [data {:data "a random payload"}
          encoded (utils/encode-secret data)
          decoded (utils/decode-secret encoded)]
      (is (= data decoded))
      (is (not= data encoded))))
  (testing "fails gracefully"
    (let [decoded (utils/decode-secret random-token)]
      (is (nil? decoded)))))

(deftest password-handling
  (testing "can hide the same password many times over"
    (let [p1 (utils/hide "password")
          p2 (utils/hide "password")]
      (is (= p1 p2)))))

(deftest repeatability
  (testing "can repeatedly encode and decode data"
    (let [data {:data "a random payload again"}
          original-encoded (utils/encode-secret data)
          original-decoded (utils/decode-secret original-encoded)]
      (is (= data original-decoded))
      (loop [i 0
             encoded (utils/encode-secret original-decoded)]
        (when (< i 1000)
          (let [decoded (utils/decode-secret encoded)]
            (is (= data decoded))
            (recur (+ 1 i)
                   (utils/encode-secret decoded))))))))

