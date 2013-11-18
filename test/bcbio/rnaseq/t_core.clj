(ns bcbio.rnaseq.t-core
  (:use
   [midje.sweet]
   [bcbio.rnaseq.util]
   [bcbio.rnaseq.htseq-combine :only [load-counts write-combined-count-file]]
   [bcbio.rnaseq.templates :only [get-analysis-fn run-template]]
   [bcbio.rnaseq.config :only [parse-bcbio-config get-analysis-config]]
   [bcbio.rnaseq.core :only [run-analyses]])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))


(def test-count-file (get-resource "htseq-count/KO_Chol_rep1.counts"))
(def count-dir (get-resource "htseq-count"))

(facts
 "facts about count-file combining"
 (fact
  "load-htseq loads a htseq-count file into an incanter.core.Dataset"
  (type (load-counts test-count-file)) => incanter.core.Dataset))


(def test-config-file (get-resource "bcbio_sample.yaml"))
(def test-out-dir "de-analysis")
(def test-count-file (str (io/file test-out-dir "combined.counts")))
(def test-config (parse-bcbio-config test-config-file test-out-dir))
(def edger-template-file (get-resource "edgeR.template"))
(defn file-exists? [f] (.exists (io/file f)))
(facts
 "facts about running template files"
 (fact
  "writing a combined count file is functional"
  (write-combined-count-file count-dir test-count-file) => test-count-file)
 (fact
  "running a single template works"
  (let
      [out-file (:out-file (run-template edger-template-file (get-analysis-config test-config)))]
    (file-exists? out-file) => true)))

(facts
 "facts about high level functions"
 (fact
  "run-analysis outputs several .tsv files"
  (let [out-maps (run-analyses test-config-file count-dir)
        out-files (map :out-file out-maps)
        normalized-files (map :normalized-file out-maps)
        r-files (map :r-file out-maps)]
    (every? file-exists? (concat out-files normalized-files r-files)) => true)))
