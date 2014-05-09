(ns bcbio.rnaseq.simulate
  (:use [bcbio.rnaseq.util]
        [bcbio.rnaseq.config]
        [bcbio.rnaseq.compare :only [compare-callers]]
        [clojure.java.shell :only [sh]]
        [clojure.tools.cli :refer [parse-opts]])
  (:require [me.raynes.fs :as fs]
            [clostache.parser :as stache]
            [clojure.string :as string]
            [bcbio.rnaseq.templates :as templates]))

(def sim-template "comparisons/simulate.template")
(def compare-template "comparisons/compare-simulated.template")

(defn simulate [out-dir sample-size library-size ngenes]
  (let [count-file (str (fs/file out-dir "sim.counts"))
        rfile (str (fs/file out-dir "sim.R"))]
    (safe-makedir out-dir)
    (spit rfile
          (stache/render-resource sim-template
                                  {:count-file (escape-quote count-file)
                                   :sample-size sample-size
                                   :library-size library-size
                                   :ngenes ngenes}))
    (sh "Rscript" rfile)
    count-file))

(defn get-analysis-template [out-dir count-file sample-size]
  {:de-out-dir out-dir
   :count-file count-file
   :comparison ["group1" "group2"]
   :conditions (concat (repeat sample-size "group1") (repeat sample-size "group2"))
   :condition-name "group1_vs_group2"
   :project "simulated"})

(defn run-one-template [analysis-template template]
  (let [analysis-config (templates/add-out-files-to-config template analysis-template)]
    (templates/run-template template analysis-config)))

(defn compare-simulated-results [sim-dir in-files]
  (let [out-file (swap-directory "concordant.pdf" sim-dir)
        score-file (str (fs/file sim-dir "sim.scores"))
        rfile (str (fs/file sim-dir "compare-simulated.R"))
        template-config {:out-file (escape-quote out-file)
                         :score-file (escape-quote score-file)
                         :in-files (seq-to-rlist in-files)
                         :project (escape-quote "simulated")}]
    (spit rfile (stache/render-resource compare-template template-config))
    (apply sh ["Rscript" "--verbose" rfile])
    out-file))

(defn run-simulation [out-dir sample-size library-size num-genes]
  (let [count-file (simulate out-dir sample-size library-size num-genes)
        analysis-template (get-analysis-template out-dir count-file sample-size)
        out-files (map :out-file (map #(templates/run-template %1 analysis-template)
                                      templates/templates))]
    (compare-callers out-files)
    (compare-simulated-results out-dir out-files)))

(def options
  [["-h" "--help"]
   ["-d" "--out-dir OUT_DIR" "Output directory"
    :default "simulate"]
   ["-s" "--sample-size SAMPLE_SIZE" "Sample size of each group"
    :default 3
    :parse-fn #(Integer/parseInt %)]
   ["-l" "--library-size SIZE" "Library size in millions of reads"
    :default 20
    :parse-fn #(Float/parseFloat %)]
   ["-n" "--num-genes GENES" "Number of genes to simulate"
    :default 10000
    :parse-fn #(Integer/parseInt %)]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage [options-summary]
  (->> [
        ""
        "Usage: bcbio-rnaseq simulate [options]"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn simulate-cli [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args options)]
    (cond
     (:help options) (exit 0 (usage summary)))
    (run-simulation (:out-dir options) (:sample-size options)
                    (:library-size options) (:num-genes options))))
