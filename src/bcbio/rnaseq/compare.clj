(ns bcbio.rnaseq.compare
  (:require [bcbio.rnaseq.config :refer :all]
            [bcbio.rnaseq.cuffdiff :as cuffdiff]
            [bcbio.rnaseq.ercc :as ercc]
            [bcbio.rnaseq.templates :refer [caller-comparison-template
                                            run-R-analyses]]
            [bcbio.rnaseq.util :refer :all]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clostache.parser :refer [render-resource]]
            [me.raynes.fs :as fs]))


(defn write-template [template hashmap]
  (let [rfile (change-extension (swap-directory template (analysis-dir)) ".R")]
    (spit rfile (render-resource template hashmap))
  rfile))


(defn make-fc-plot [in-files]
  "create a fold change plot comparing to the seqc qPCR data"
  (let [template-file "comparisons/qPCR_foldchange.template"
        out-file (swap-directory "fc-plot.pdf" (analysis-dir))
        qpcr-file (copy-resource
                   "seqc/qPCR/qpcr_HBRR_vs_UHRR.tidy" (analysis-dir))
        template-config {:out-file (escape-quote out-file)
                         :qpcr-file (escape-quote qpcr-file)
                         :in-files (seq-to-rlist in-files)
                         :project (escape-quote (project-name))}]
    (apply sh ["Rscript" (write-template template-file template-config)])
    out-file))

(def options
  [["-h" "--help"]
   ["-n" "--cores CORES" "Number of cores"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(> % 0)]]
   [nil "--counts-only" "Only run count-based analyses"]
   [nil "--seqc" "Data is from a SEQC alignment"]])

(defn usage [options-summary]
  (->> [
        ""
        "Usage: bcbio-rnaseq compare [options] project-file key"
        ""
        "Options:"
        options-summary
        ""
        "Arguments:"
        "  project-file    A bcbio-nextgen project file"
        "  key             Key in the metadata field to do pairwise comparisons"
        ""]
       (string/join \newline)))

(defn run-comparisons [key cores counts-only]
  (let [result-files (map :out-file (run-R-analyses key))]
    (if counts-only
      (make-fc-plot result-files)
      (make-fc-plot (conj result-files (:out-file (cuffdiff/run key cores)))))))



(defn run-callers [key cores counts-only]
  (let [result-files (map :out-file (run-R-analyses key))]
    (if counts-only
      result-files
      (conj result-files (:out-file (cuffdiff/run key cores))))))


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

(defn compare-bcbio-run [seqc counts-only cores project-file key]
  (setup-config project-file)
  (safe-makedir (analysis-dir))
  (if seqc
    (run-comparisons (keyword key) cores counts-only)
    (compare-callers (run-callers (keyword key) counts-only cores))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn compare-cli [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args options)]
    (cond
     (:help options) (exit 0 (usage summary))
     (not= (count arguments) 2) (exit 1 (usage summary)))
    (compare-bcbio-run (:seqc options) (:counts-only options)
                       (:cores options) (first arguments) (second arguments))))
