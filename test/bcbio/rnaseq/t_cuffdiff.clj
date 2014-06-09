(ns bcbio.rnaseq.t-cuffdiff
  (:require
   [bcbio.rnaseq.t-cuffdiff :refer :all]
   [bcbio.rnaseq.cuffdiff :as cuffdiff]
   [bcbio.rnaseq.util :refer [file-exists?]]
   [bcbio.rnaseq.test-setup :refer [test-setup]]
   [clojure.test :refer :all]))

(use-fixtures :once test-setup)

(deftest cuffdiff-works
  (is (file-exists? (:out-file (cuffdiff/run :panel 1)))))
