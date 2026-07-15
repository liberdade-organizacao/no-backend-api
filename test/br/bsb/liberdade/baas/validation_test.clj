(ns br.bsb.liberdade.baas.validation-test
  (:require [clojure.test :refer [deftest is]]
            [br.bsb.liberdade.baas.validation :as v]))

(deftest validate-presence-test
  (is (nil? (v/validate-presence "something")))
  (is (= "is required" (v/validate-presence nil))))

(deftest validate-string-test
  (is (nil? (v/validate-string "hello")))
  (is (= "must be a string" (v/validate-string 123))))

(deftest validate-email-test
  (is (nil? (v/validate-email "test@example.com")))
  (is (= "must be a valid email address" (v/validate-email "not-an-email")))
  (is (= "must be a valid email address" (v/validate-email 123))))

(deftest required-string-test
  (is (nil? (v/required-string "hello")))
  (is (= "is required" (v/required-string nil)))
  (is (= "must be a string" (v/required-string 123))))

(deftest required-email-test
  (is (nil? (v/required-email "test@example.com")))
  (is (= "is required" (v/required-email nil)))
  (is (= "must be a valid email address" (v/required-email "not-an-email"))))

(deftest validate-test
  (let [schema {"email" v/validate-email
                "password" v/validate-string}]
    (is (= {"email" "test@example.com" "password" "password123"}
           (v/validate schema {"email" "test@example.com" "password" "password123"})))
    (is (= {:error "Validation Failed"
            :details {"email" "must be a valid email address"}}
           (v/validate schema {"email" "not-an-email" "password" "password123"})))
    (is (= {:error "Validation Failed"
            :details {"password" "must be a string"}}
           (v/validate schema {"email" "test@example.com" "password" 123})))))
