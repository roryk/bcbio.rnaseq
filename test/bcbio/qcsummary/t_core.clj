(ns bcbio.qcsummary.t-core
  (:require [bcbio.rnaseq.util :as util]
            [incanter.core :as ic])
  (:use [midje.sweet]
        [bcbio.qcsummary.core]))

(def test-project-summary (util/get-resource "project/project-summary.yaml"))

(facts "about qcsummary" :unit
  (fact
    "loading the project file doesn't throw an error"
    (load-summary test-project-summary) => truthy)
  (fact
     "the project file has samples field"
     (:samples (load-summary test-project-summary)) => truthy)
  (let
      [summaries (map summary (:samples (load-summary test-project-summary)))]
    (fact
     "we can get a summary from a list of samples"
     summaries => truthy)
    (fact
     "we can get a dataframe from a list of samples" :unit
     (first (ic/col-names (tidy-summary summaries))) => "Name")
    (fact
     "tidy-summary returns a full dataframe" :unit
     (ic/nrow (tidy-summary summaries)) => 10)))


