(ns bcbio.qcsummary.t-core
  (:require [bcbio.rnaseq.util :as util]
            [incanter.core :as ic]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [bcbio.qcsummary.core :as qc])
  (:use [midje.sweet]))

(def test-project-summary (util/get-resource "project/project-summary.yaml"))

;; (facts "about summarize" :unit
;;   (let [summary-dir (-> test-project-summary util/dirname (io/file "summary") str)]
;;     (with-state-changes [(after :facts (fs/delete-dir summary-dir))]
;;       (fact
;;        "running summary without a formula works"
;;        (util/file-exists? (qc/summarize test-project-summary nil)) => truthy)
;;       (fact
;;        "running summary with a formula works"
;;        (util/file-exists? (qc/summarize test-project-summary "~ panel")) => truthy))))

(facts "about qcsummary" :unit
  (fact
    "loading the project file doesn't throw an error"
    (qc/load-summary test-project-summary) => truthy)
  (fact
     "the project file has samples field"
     (:samples (qc/load-summary test-project-summary)) => truthy)
  (let
      [summaries (map qc/summary (:samples (qc/load-summary test-project-summary)))]
    (fact
     "we can get a summary from a list of samples"
     summaries => truthy)
    (fact
     "we can get a dataframe from a list of samples" :unit
     (first (ic/col-names (qc/tidy-summary summaries))) => "Name")
    (fact
     "tidy-summary returns a full dataframe" :unit
     (ic/nrow (qc/tidy-summary summaries)) => 10)))



