(ns bcbio.rnaseq.t-compare
  (:require [bcbio.rnaseq.t-compare :refer :all]
            [bcbio.rnaseq.util :refer [dirname get-resource file-exists?]]
            [bcbio.rnaseq.compare :as compare]
            [bcbio.rnaseq.config :refer [alter-config! get-config analysis-dir]]
            [me.raynes.fs :as fs]
            [bcbio.rnaseq.core :as core]
            [bcbio.rnaseq.test-setup :refer [test-setup default-bcbio-project]]
            [clojure.test :refer :all]))

(use-fixtures :once test-setup)

(deftest comparison-plot-from-project-works
  (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
        in-files (fs/glob (str dirname "*_vs_*.tsv"))]
    (is (file-exists? (:fc-plot (compare/compare-callers in-files))))))

(deftest seqc-plots-work
  (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
        in-files (fs/glob (str dirname "*_vs_*.tsv"))]
    (alter-config! (assoc (get-config) :analysis-dir dirname))
    (is (file-exists? (compare/make-fc-plot in-files)))))

(deftest combining-R-and-cuffdiff-works
  (is (file-exists? (compare/run-comparisons :panel 1 false))))

(deftest bcbio-comparison-cli-works
 (let [out-map (core/-main "compare" (default-bcbio-project) "panel")]
   (is (file-exists? (:fc-plot out-map)))))

(deftest comparison-plot-works
  (let [in-files (map str (fs/glob (fs/file (analysis-dir) "*_vs_*.tsv")))]
    (is (file-exists? (compare/make-fc-plot in-files)))))

