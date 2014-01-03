(ns bcbio.rnaseq.ercc
  (:use [incanter.io :only [read-dataset]]
        [incanter.core :only [sel]]
        [bcbio.rnaseq.compare :only [write-template]]
        [bcbio.rnaseq.util]
        [bcbio.rnaseq.config]
        [clojure.java.shell :only [sh]])
  (:require [me.raynes.fs :as fs]))

(def ercc-template #(str (get-resource "comparisons/ERCC.template")))
(def ercc-known #(str (get-resource "seqc/ERCC/ERCC_Controls_Analysis.txt")))

(defn ercc-analysis? [in-files]
  "returns non-nil if in-files look like they have ERCC data in them"
  (let [test-data (read-dataset (first in-files) :delim \tab :header true)
        test-id "ERCC-00002"]
    (some #{test-id} (sel test-data :cols :id))))

(defn ercc-analysis [in-files]
  (let [out-file (str (fs/file (analysis-dir) "ERCC_concordance.tsv"))
        config {:in-files (seq-to-rlist in-files)
                :out-dir (analysis-dir)
                :ercc-file (ercc-known)
                :out-file out-file}
        template-config (apply-to-keys config escape-quote
                                       :out-dir :ercc-file :out-file)]
    (apply sh ["Rscript" (write-template (ercc-template) template-config)])
    out-file))
