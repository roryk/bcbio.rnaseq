(ns bcbio.rnaseq.core
  (:use [bcbio.rnaseq.config :only [parse-bcbio-config]]
        [bcbio.rnaseq.templates :only [get-analysis-fn templates]]
        [bcbio.rnaseq.util]
        [midje.sweet]))

(defn run-analyses [bcbio-config-file]
  (let [bcbio-config (parse-bcbio-config bcbio-config-file)
        analyze (get-analysis-fn bcbio-config)]
    (map :out-file (map analyze templates))))


;;
(def test-config-file (get-resource "bcbio_sample.yaml"))
(def test-out-dir "de-analysis")
(fact
 "run-analysis outputs several .tsv files"
 (run-analyses test-config-file) => (map str (get-files-with-extension test-out-dir ".tsv")))
