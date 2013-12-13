(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.templates :only [get-analysis-fn templates]]
        [bcbio.rnaseq.config]
        [bcbio.rnaseq.util]
        [clojure.tools.cli :only [cli]]
        [bcbio.rnaseq.compare :only [make-fc-plot]])
  (:require [bcbio.rnaseq.htseq-combine :as combine-counts]
            [clojure.java.io :as io])
  (:gen-class :main true))

(defn run-analyses [key]
  "run all of the template files using key as the comparison field in the
   metadata entries"
  (combine-counts/write-combined-count-file (count-files) (combined-count-file))
  (map (get-analysis-fn key) templates))

(defn run-comparisons [key]
  (map make-fc-plot (run-analyses key)))

(defn compare-callers [bcbio-project-file]
  )

(defn -main [cur-type & args]
  (apply (case (keyword cur-type)
           :combine-counts combine-counts/cl-entry
           :run-comparisons run-comparisons
           :compare-callers compare-callers)
         args))
