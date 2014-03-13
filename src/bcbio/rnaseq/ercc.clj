(ns bcbio.rnaseq.ercc
  (:use [incanter.io :only [read-dataset]]
        [incanter.core :only [sel]]
        [bcbio.rnaseq.util]
        [bcbio.rnaseq.config]
        [clostache.parser :only [render-resource]]
        [clojure.java.shell :only [sh]])
  (:require [me.raynes.fs :as fs]))

(defn write-template [template hashmap]
  (let [rfile (change-extension (swap-directory template (analysis-dir)) ".R")]
    (spit rfile (render-resource template hashmap))
  rfile))

(def ercc-template "comparisons/ERCC.template")
(def ercc-known (get-resource "seqc/ERCC/ERCC_Controls_Analysis.txt"))

(defn ercc-analysis? [in-files]
  "returns non-nil if in-files look like they have ERCC data in them"
  (let [test-data (read-dataset (first in-files) :delim \tab :header true)
        test-id "ERCC-00002"]
    (some #{test-id} (sel test-data :cols :id))))

(defn ercc-analysis [in-files]
  (let [out-file (str (fs/file (analysis-dir) "ERCC_concordance.tsv"))
        config {:in-files (seq-to-rlist in-files)
                :out-dir (analysis-dir)
                :ercc-file ercc-known
                :out-file out-file}
        template-config (apply-to-keys config escape-quote
                                       :out-dir :ercc-file :out-file)]
    (apply sh ["Rscript" (write-template ercc-template template-config)])
    out-file))
