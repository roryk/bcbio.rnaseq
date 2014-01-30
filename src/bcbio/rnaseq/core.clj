(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.templates :only [get-analysis-fn templates
                                       caller-comparison-template]]
        [bcbio.rnaseq.config]
        [bcbio.rnaseq.util]
        [bcbio.rnaseq.compare :only [make-fc-plot write-template]]
        [clojure.java.shell :only [sh]])
  (:require [bcbio.rnaseq.htseq-combine :as combine-counts]
            [bcbio.rnaseq.cufflinks :as cufflinks]
            [bcbio.rnaseq.ercc :as ercc]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class :main true))

(defn run-R-analyses [key]
  "run all of the template files using key as the comparison field in the
   metadata entries"
  (combine-counts/write-combined-count-file (count-files) (combined-count-file))
  (map (get-analysis-fn key) templates))

(defn run-cuffdiff [key cores]
  (let [out-file (str (fs/file (analysis-dir)
                               (str "cuffdiff_"
                                    (comparison-name key) ".tsv")))]
    (when-not (file-exists? out-file)
      (cufflinks/write-gene-info (cufflinks/run-cuffdiff cores key) out-file))
    {:out-file out-file}))

(defn run-comparisons [key cores]
  (let [cuffdiff-out (run-cuffdiff key cores)
        r-analyses-out (run-R-analyses key)
        result-files (conj (map :out-file r-analyses-out)
                           (:out-file cuffdiff-out))]
    (make-fc-plot result-files)))

(defn run-callers [key cores]
  (let [cuffdiff-out (run-cuffdiff key cores)
        r-analyses-out (run-R-analyses key)]
    (conj (map :out-file r-analyses-out) (:out-file cuffdiff-out))))

(defn compare-callers [in-files]
  (let [out-dir (dirname (first in-files))
        config {:in-files (seq-to-rlist in-files)
                :fc-plot (str (fs/file out-dir "logFC_comparison_plot.pdf"))
                :expr-plot (str (fs/file out-dir "log10expr_comparison_plot.pdf"))
                :pval-plot (str (fs/file out-dir "pval_comparison_plot.pdf"))
                :out-dir out-dir}
        template-config (apply-to-keys config escape-quote
                                       :fc-plot :expr-plot :pval-plot :out-dir)]
    (apply sh ["Rscript" (write-template caller-comparison-template template-config)])
    (when (ercc/ercc-analysis? in-files) (assoc config :ercc-file
                                                (ercc/ercc-analysis in-files)))
    config))


(defn compare-bcbio-run [project-file key cores]
  (setup-config project-file)
  (compare-callers (run-callers key cores)))

(def compare-bcbio-run-options
  [["-h" "--help"]
   ["-n" "--cores CORES" "Number of cores"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(> 0)]]])

(defn compare-bcbio-cl-entry [& args]
  (let [cli-options (parse-opts args compare-bcbio-run-options)
        [project-file key] (:arguments cli-options)
        cores (get-in cli-options [:options :cores])]
    (compare-bcbio-run project-file (keyword key) cores)))

(defn -main [cur-type & args]
  (apply sh ["Rscript" (get-resource "scripts/install_libraries.R")])
  (apply (case (keyword cur-type)
           :compare-bcbio-run compare-bcbio-cl-entry
           :combine-counts combine-counts/cl-entry
           :seqc-comparisons run-comparisons
           :compare-callers compare-callers)
         args))
