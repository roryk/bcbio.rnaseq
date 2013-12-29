(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.templates :only [get-analysis-fn templates
                                       caller-comparison-template]]
        [bcbio.rnaseq.config]
        [bcbio.rnaseq.util]
        [bcbio.rnaseq.compare :only [make-fc-plot write-template]]
        [clojure.java.shell :only [sh]])
  (:require [bcbio.rnaseq.htseq-combine :as combine-counts]
            [bcbio.rnaseq.cufflinks :as cufflinks]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class :main true))

(defn run-R-analyses [key]
  "run all of the template files using key as the comparison field in the
   metadata entries"
  (combine-counts/write-combined-count-file (count-files) (combined-count-file))
  (map (get-analysis-fn key) templates))

(defn run-cuffdiff [key]
  (let [out-file (str (fs/file (analysis-dir)
                               (str "cuffdiff_"
                                    (comparison-name key) ".tsv")))]
    (when-not (file-exists? out-file)
      (cufflinks/write-gene-info (cufflinks/run-cuffdiff 1 key) out-file))
    {:out-file out-file}))

(defn run-comparisons [key]
  (let [cuffdiff-out (run-cuffdiff key)
        r-analyses-out (run-R-analyses key)
        result-files (conj (map :out-file r-analyses-out)
                           (:out-file cuffdiff-out))]
    (make-fc-plot result-files)))

(defn run-callers [key]
  (let [cuffdiff-out (run-cuffdiff key)
        r-analyses-out (run-R-analyses key)]
    (conj (map :out-file r-analyses-out) (:out-file cuffdiff-out))))

(defn compare-callers [in-files]
  (let [out-dir (dirname (first in-files))
        config {:in-files (seq-to-rlist in-files)
                :fc-plot (str (fs/file out-dir "logFC_comparison_plot.pdf"))
                :expr-plot (str (fs/file out-dir "log10expr_comparison_plot.pdf"))
                :pval-plot (str (fs/file out-dir "pval_comparison_plot.pdf"))}
        template-config (apply-to-keys config escape-quote
                                       :fc-plot :expr-plot :pval-plot)]
    (apply sh ["Rscript" (write-template caller-comparison-template template-config)])
    config))

(defn compare-bcbio-run [project-file key]
  (setup-config project-file)
  (compare-callers (run-callers key)))

(defn compare-bcbio-cl-entry [& args]
  (let [[project-file key] (:arguments (parse-opts args [["-h" "--help"]]))]
    (compare-bcbio-run project-file (keyword key))))

(defn main [cur-type & args]
  (apply sh ["Rscript" (get-resource "scripts/install_libraries.R")])
  (apply (case (keyword cur-type)
           :compare-bcbio-run compare-bcbio-cl-entry
           :combine-counts combine-counts/cl-entry
           :run-comparisons run-comparisons
           :compare-callers compare-callers)
         args))
