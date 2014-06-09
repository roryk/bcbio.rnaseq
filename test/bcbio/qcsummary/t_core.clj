(ns bcbio.qcsummary.t-core
  (:require [bcbio.rnaseq.util :as util]
            [incanter.core :as ic]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [bcbio.qcsummary.core :as qc]))


(def test-project-summary (util/get-resource "project/project-summary.yaml"))
(def summary-dir (-> test-project-summary util/dirname (io/file "summary") str))

(defn summary-fixture [f]
  (f)
  (util/rmdir summary-dir))

(use-fixtures :each summary-fixture)

;; (facts "about summarize" :unit
;;   (let [summary-dir (-> test-project-summary util/dirname (io/file "summary") str)]
;;     (with-state-changes [(after :facts (fs/delete-dir summary-dir))]
;;       (fact
;;        "running summary without a formula works"
;;        (util/file-exists? (qc/summarize test-project-summary nil)) => truthy)
;;       (fact
;;        "running summary with a formula works"
;;        (util/file-exists? (qc/summarize test-project-summary "~ panel")) => truthy))))

(def not-nil? (complement nil?))

(deftest loading-project-file-works
  (is (not-nil? (qc/load-summary test-project-summary))))

(deftest project-file-has-samples-field
  (is (not-nil? (:samples (qc/load-summary test-project-summary)))))

(deftest summary-from-samples-works
  (is (not-nil?
       (map qc/summary (:samples (qc/load-summary test-project-summary))))))

(deftest tidy-summary-returns-dataframe
  (let
      [summaries (map qc/summary (:samples (qc/load-summary test-project-summary)))]
    (is (= 10 (ic/nrow (qc/tidy-summary summaries))))))

(deftest summary-with-formula-works
  (is (util/file-exists? (qc/summarize test-project-summary "~ panel"))))

(deftest summary-without-formula-works
  (is (util/file-exists? (qc/summarize test-project-summary nil))))
