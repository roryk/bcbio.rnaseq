(ns bcbio.rnaseq.t-core
  (:use midje.sweet
        bcbio.rnaseq.core
        incanter.core)
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(def test-config-file (get-resource "bcbio_sample.yaml"))
(def test-config (get-config test-config-file))
(def test-combined-count-file (get-resource "htseq-count/combined.counts"))

(facts "about `configuration file parsing`"
  (fact "each sample has descriptions of the data in it"
    (first (get-descriptions test-config)) => "WT_Con_rep1"
    (last (get-descriptions test-config)) => "KO_Chol_rep3")
  (fact "each sample has a condition status associated with it"
    (first (get-condition test-config)) => "control"
    (last (get-condition test-config)) => "cholesterol")
  (fact "we can derive the count-files location from the config files"
    (-> test-config-file htseq-files first base-stem) => "WT_Con_rep1"))

(facts "about `htseq-file parsing`"
  (fact "load-htseq loads a htseq-count file into an incanter.core.Dataset"
    (type (load-htseq
           (-> test-config-file htseq-files first))) => incanter.core.Dataset)
  (fact "combine-count-files combines all htseq-files in a directory into a single
  incanter.core.Dataset"
    (type (combine-count-files
           (htseq-files test-config-file))) => incanter.core.Dataset)
  (fact "write-combined-count-file outputs a htseq-count file named combined.counts"
    (fs/base-name (write-combined-count-file
                  (htseq-files test-config-file))) => "combined.counts")
  (fact "write-combined-count-file outputs a htseq-count file we can load"
       (type (load-htseq (write-combined-count-file
                          (htseq-files test-config-file)))) => incanter.core.Dataset))

(def edger-template-file (get-resource "edgeR.template"))
(facts "facts about templating"
       (fact "get-analysis-fn returns a function to run an analysis"
             (fs/base-name ((get-analysis-fn config)
                        edger-template-file)) => "edgeR.R")
       (fact "we can do all the analyses at once"
             (map (get-analysis-fn config) templates) => (map
                                                           #(change-extension % ".R")
                                                           templates)))








