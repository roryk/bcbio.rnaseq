(ns bcbio.rnaseq.t-core
  (:use
   [midje.sweet]
   [bcbio.rnaseq.util]
   [bcbio.rnaseq.htseq-combine :only [load-counts write-combined-count-file]]
   [bcbio.rnaseq.templates :only [templates get-analysis-config run-template]]
   [bcbio.rnaseq.config]
   [bcbio.rnaseq.compare :only [make-fc-plot]]
   [bcbio.rnaseq.cufflinks :only [run-cuffdiff]])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [bcbio.rnaseq.core :as core]))

(setup-config default-bcbio-project)

(facts
 "facts about template files"
 (fact
  "running a single template file is functional"
  (let [template (first templates)
        analysis-config (get-analysis-config :panel)]
    (write-combined-count-file (count-files) (combined-count-file))
    (file-exists? (:out-file (run-template template analysis-config))) => true))
 (fact
  "running a group of analyses produces output files"
  (every? file-exists? (map :out-file (core/run-R-analyses :panel))) => true)
 (fact
  "making the comparison plot automatically works"
  (let [in-files (map str (fs/glob (fs/file (analysis-dir) "*.tsv")))]
    (file-exists? (make-fc-plot in-files)) => true)))

(facts
 "facts about cufflinks"
  (fact
  "running Cuffdiff works"
  (file-exists? (:out-file (core/run-cuffdiff :panel))) => true))

(fact
 "combining R analyses and cuffdiff works"
 (file-exists? (core/run-comparisons :panel)) => true)

(fact
 "making the seqc plots work"
 (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
       in-files (fs/glob (str dirname "*.tsv"))]
   (alter-config! (assoc (get-config) :analysis-dir dirname))
   (make-fc-plot in-files) => true))
