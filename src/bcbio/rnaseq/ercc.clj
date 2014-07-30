(ns bcbio.rnaseq.ercc
  (:require [bcbio.rnaseq.config :refer :all]
            [bcbio.rnaseq.util :refer :all]
            [clojure.java.shell :refer [sh]]
            [clostache.parser :refer [render-resource]]
            [incanter.core :refer [sel]]
            [incanter.io :refer [read-dataset]]
            [me.raynes.fs :as fs]))

(defn write-template [template hashmap out-dir]
  (let [rfile (change-extension (swap-directory template out-dir) ".R")]
    (spit rfile (render-resource template hashmap))
  rfile))

(def ercc-template "comparisons/ERCC.template")
(def ercc-known (get-resource "seqc/ERCC/ERCC_Controls_Analysis.txt"))

(defn ercc-analysis? [in-files]
  "returns non-nil if in-files look like they have ERCC data in them"
  (let [test-data (read-dataset (first in-files) :delim \tab :header true)
        test-id "ERCC-00002"]
    (some #{test-id} (sel test-data :cols :id))))

(defn ercc-analysis
  ([in-files] (ercc-analysis in-files (analysis-dir)))
  ([in-files out-dir]
     (let [out-file (str (fs/file out-dir "ERCC_concordance.tsv"))
           config {:in-files (seq-to-rlist in-files)
                   :out-dir out-dir
                   :ercc-file ercc-known
                   :out-file out-file}
           template-config (apply-to-keys config escape-quote
                                          :out-dir :ercc-file :out-file)]
       (apply sh ["Rscript" (write-template ercc-template template-config out-dir)])
       out-file)))
