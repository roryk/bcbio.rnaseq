(ns bcbio.rnaseq.t-core
  (:use
   [midje.sweet]
   [bcbio.rnaseq.util]
   [bcbio.rnaseq.htseq-combine :only [load-counts write-combined-count-file]]
   [bcbio.rnaseq.templates :only [get-analysis-fn]]
   [bcbio.rnaseq.config :only [parse-bcbio-config]]
   [bcbio.rnaseq.core :only [run-analyses]])
  (:require [clojure.java.io :as io]))

(def test-count-file (get-resource "htseq-count/KO_Chol_rep1.counts"))
(def count-dir (get-resource "htseq-count"))

(facts
 "facts about count-file combining"
 (fact
  "load-htseq loads a htseq-count file into an incanter.core.Dataset"
  (type (load-counts test-count-file)) => incanter.core.Dataset)

 (fact
  "write-combined-count-file is functional"
  (let [out-file (io/file count-dir "combined" "combined.counts")]
    (write-combined-count-file count-dir out-file) => out-file)))

(def test-config-file (get-resource "bcbio_sample.yaml"))
(def test-out-dir "de-analysis")
(def test-config (parse-bcbio-config test-config-file test-out-dir))
(def edger-template-file (get-resource "edgeR.template"))
(facts
 "facts about running template files"
 (fact
  "get-analysis function returns a function to run an analysis"
  (:out-file ((get-analysis-fn test-config)
              edger-template-file)) =>
              (str (io/file test-out-dir "edgeR_control_vs_cholesterol.tsv"))))


(def test-config-file (get-resource "bcbio_sample.yaml"))
(def test-out-dir "de-analysis")
(facts
 "facts about high level functions"
 (fact
  "run-analysis outputs several .tsv files"
  (run-analyses test-config-file) => (map str (get-files-with-extension test-out-dir ".tsv"))))
