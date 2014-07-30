(ns bcbio.rnaseq.t-template
  (:require
   [bcbio.rnaseq.util :refer [file-exists?]]
   [bcbio.rnaseq.test-setup :refer [test-setup]]
   [bcbio.rnaseq.config :refer [combined-count-file count-files]]
   [bcbio.rnaseq.templates :refer [run-template templates get-analysis-config
                                  run-R-analyses]]
   [bcbio.rnaseq.htseq-combine :refer [write-combined-count-file]]
   [clojure.test :refer :all]
   [bcbio.rnaseq.t-template :refer :all]))

(use-fixtures :once test-setup)

(deftest running-single-template-works
  (let [template (first templates)
        analysis-config (get-analysis-config :panel)]
    (write-combined-count-file (count-files) (combined-count-file))
    (is (file-exists? (:out-file (run-template template analysis-config))))))

(deftest running-group-templates-works
  (is (every? file-exists? (map :out-file (run-R-analyses :panel)))))
