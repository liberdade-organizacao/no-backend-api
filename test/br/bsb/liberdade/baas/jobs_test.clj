(ns br.bsb.liberdade.baas.jobs-test
  (:require [clojure.test :refer :all]
            [br.bsb.liberdade.baas.test-helpers :as th]
            [br.bsb.liberdade.baas.jobs :as jobs]))

(use-fixtures :each th/database-fixture)

#_(deftest from-recfile-loads-database
    (testing "from recfile can load a file with multiple tables"
      (let [input-file "resources/test_file.rec"
            _ (jobs/from-recfile input-file)
            output-file "resources/test_output.rec"
            _ (jobs/to-recfile output-file)]
        (is true))))

