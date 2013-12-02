(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.config :only [parse-bcbio-config]]
        [bcbio.rnaseq.templates :only [get-analysis-fn templates]]
        [bcbio.rnaseq.util]
        [clojure.tools.cli :only [cli]])
  (:require [bcbio.rnaseq.htseq-combine :as combine-counts]
            [clojure.java.io :as io])
  (:gen-class :main true))

(defn get-count-files [bcbio-config count-dir]
  (let [descriptions (map :description (:details bcbio-config))]
    (map #(str % ".counts") (map #(io/file count-dir %) descriptions))))

(defn run-analyses [bcbio-config-file count-dir]
  (let [bcbio-config (parse-bcbio-config bcbio-config-file)
        analyze (get-analysis-fn bcbio-config)
        combined-file (:count-file bcbio-config)
        count-files (get-count-files bcbio-config count-dir)]
    (combine-counts/write-combined-count-file count-files combined-file)
    (map analyze templates)))


(defn -main [cur-type & args]
  (apply (case (keyword cur-type)
           :combine-counts combine-counts/cl-entry)
         args))
