(ns br.bsb.liberdade.baas.api-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [br.bsb.liberdade.baas.db :as db]))

(def test-note
  (str "# Introduction Note\n"
       "- List item #1\n"
       "- List item #2\n"
       "A paragraph as example\n"))
(def another-test-note
  (str "Another test note, nothing serious"))
(def random-auth-key
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MTIzNH0.rWp8vvb4aDZAGcHEYjhCe9qaaf8mSyvyLeyC1QuZWU0")

#_(deftest database-to-json
    (testing "database backup can be exported to JSON"
      (db/setup-database)
      (db/create-admin "admin" "secretpassword" test-note)
      (db/create-user "username" "password" another-test-note)
      (let [auth (db/auth-user "admin" "secretpassword")
            backup (-> auth (get "auth_key") (db/backup))
            json-representation (json/write-str (:database backup))]
        (is (not= "Don't know how to write JSON of class java.sql.Timestamp" json-representation)))
      (db/drop-database)))

