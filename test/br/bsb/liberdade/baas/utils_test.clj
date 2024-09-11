(ns br.bsb.liberdade.baas.utils-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.utils :as utils]))

(def random-token
  "XZ69WpRTqZgEOqCJqaOK4iOKGLkg505VSASQ8MMGWs3mn1p6U81FvB5rSLpKlIjkZTUIBC6KiHIboy")

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

(deftest measurability
  (testing "encoded data can be measured"
    (let [data "abcdefghij" 
          encoded (utils/encode-data data)
          length (count encoded)]
      (is (= 16 length)))))
