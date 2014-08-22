(ns bcbio.rnaseq.t-simulate
  (:require [bcbio.rnaseq.t-simulate :refer :all]
            [bcbio.rnaseq.simulate :as simulate]
            [bcbio.rnaseq.simulator :as simulator]
            [bcbio.rnaseq.test-setup :refer [test-setup]]
            [clojure.test :refer :all]
            [bcbio.rnaseq.util :as util]))

(deftest simulate-nofile-works
  (is (util/file-exists? (simulate/run-simulation "simulate" 10000 3 20 nil))))

(deftest simulate-file-works
  (is (util/file-exists?
       (simulate/run-simulation "simulate" 10000 3 20 simulator/default-count-file))))
