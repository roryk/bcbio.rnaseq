(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.templates :only [caller-comparison-template run-R-analyses templates]]
        [bcbio.rnaseq.config]
        [bcbio.rnaseq.util]
        [bcbio.rnaseq.compare :only [make-fc-plot write-template]]
        [clojure.java.shell :only [sh]])
  (:require [bcbio.rnaseq.cuffdiff :as cuffdiff]
            [bcbio.rnaseq.ercc :as ercc]
            [bcbio.rnaseq.simulate :as simulate]
            [me.raynes.fs :as fs]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string])
  (:gen-class :main true))

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

(defn run-simulation []
  (let [count-file (simulate/simulate)
        analysis-template (simulate/get-analysis-template (simulate/sim-dir) count-file)
        out-files (map :out-file (map (partial simulate/run-one-template analysis-template)
                                      templates))]
    (compare-callers out-files)
    (simulate/compare-simulated-results out-files)))

(def options
  [["-h" "--help"]
   ["-n" "--cores CORES" "Number of cores"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(> % 0)]]
   [nil "--simulate" "Run on simulated data"]
   [nil "--counts-only" "Only run count-based analyses"]
   [nil "--seqc" "Data is from a SEQC alignment"]])

(defn usage [options-summary]
  (->> [
    ""
    "Usage: bcbio-rnaseq [options] bcbio-project-file"
    ""
    "Options:"
    options-summary
    ]
    (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn compare-bcbio-run [seqc counts-only cores project-file key]
  (setup-config project-file)
  (if seqc
    (run-comparisons (keyword key) cores)
    (compare-callers (run-callers (keyword key) counts-only cores))))


(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args options)]
    (cond
      (:help options) (exit 0 (usage summary))
      (:simulate options) (run-simulation)
      (not= (count arguments) 2) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    (compare-bcbio-run (:seqc options) (:counts-only options)
                       (:cores options) (first arguments) (second arguments))))
