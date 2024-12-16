(ns br.bsb.liberdade.baas.db-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.db :as db]))

(defn- database-fixture [f]
  (db/setup-database)
  (db/run-migrations)
  (try
    (f)
    (catch Exception e
      (println e))
    (finally
      (db/drop-database))))

(deftest all-migrations-are-executed-correctly
  (testing "all migrations are included"
    (let [fx (atom 0)
          side-fx #(swap! fx inc)]
      (database-fixture side-fx)
      (is (= 1 @fx)))))

