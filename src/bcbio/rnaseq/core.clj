(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.config :only [parse-bcbio-config]]
        [bcbio.rnaseq.templates :only [get-analysis-fn templates]]
        [bcbio.rnaseq.util]
        [clojure.tools.cli :only [cli]])
  (:require [bcbio.rnaseq.htseq-combine :as combine-counts])
  (:gen-class :main true))

(defn run-analyses [bcbio-config-file]
  (let [bcbio-config (parse-bcbio-config bcbio-config-file)
        analyze (get-analysis-fn bcbio-config)]
    (map :out-file (map analyze templates))))

(defn -main [cur-type & args]
  (apply (case (keyword cur-type)
           :combine-counts combine-counts/cl-entry)
         args))
