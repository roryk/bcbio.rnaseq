(ns bcbio.rnaseq.t-core
  (:use
   [midje.sweet]
   [bcbio.rnaseq.util]
   [bcbio.rnaseq.htseq-combine :only [load-counts write-combined-count-file]]
   [bcbio.rnaseq.templates :only [templates get-analysis-config run-template]]
   [bcbio.rnaseq.config]
   [bcbio.rnaseq.core :only [run-analyses run-comparisons]]
   [bcbio.rnaseq.compare :only [make-fc-plot]])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(setup-config default-bcbio-project)

(facts
 "facts about template files"
 (fact
  "running a single template file is functional"
  (let [template (first templates)
        analysis-config (get-analysis-config :condition)]
    (write-combined-count-file (count-files) (combined-count-file))
    (file-exists? (:out-file (run-template template analysis-config))) => true))
 (fact
  "running a group of analyses produces output files"
  (run-analyses :condition) => true))


;; (facts
;;  "facts about running template files"
;;  (fact
;;   "writing a combined count file is functional"
;;   (write-combined-count-file count-dir test-count-file) => test-count-file)
;;  (fact
;;   "running a single template works"
;;   (let
;;       [out-file (:out-file (run-template edger-template-file (get-analysis-config test-config)))]
;;     (file-exists? out-file) => true)))

;; (def seqc-dir (get-resource "seqc/seqc-counts"))
;; (def test-config-file (get-resource "seqc/bcbio_sample_config.yaml"))
;; (facts
;;  "facts about high level functions"
;;  (fact
;;   "run-analysis outputs several .tsv files"
;;   (let [out-maps (run-analyses test-config-file seqc-dir)
;;         out-files (map :out-file out-maps)
;;         normalized-files (map :normalized-file out-maps)
;;         r-files (map :r-file out-maps)]
;;     (every? file-exists? (concat out-files normalized-files r-files)) => true)))

;; (fact
;;  "run-comparisons outputs a .pdf file"
;;  (let [analysis-dir (get-resource "test-analysis")]
;;    (file-exists? (make-fc-plot analysis-dir)) => true))
