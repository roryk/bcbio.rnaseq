(ns bcbio.rnaseq.htseq-combine
  (:use [incanter.io :only [read-dataset]]
        [incanter.core :only [to-dataset sel conj-cols col-names save]]
        [bcbio.rnaseq.util]
        [midje.sweet])
  (:require [clojure.java.io :as io]))

(defn- get-count-files [count-dir]
  (get-files-with-extension count-dir ".counts"))

(defn- load-counts [count-file]
  "load a tab delimited count-file with the first column as the identifier"
  (read-dataset count-file :delim \tab))

(defn- extract-count-column [count-file]
  (to-dataset (sel (load-counts count-file) :cols 1)))

(defn- merge-counts [combined count-file]
  (conj-cols combined (extract-count-column count-file)))

(defn combine-count-files [count-dir]
  "loads *.counts files in a directory into a single incanter Dataset object"
  (let [count-files (get-count-files count-dir)]
     (col-names
      (reduce merge-counts (load-counts (first count-files)) (rest count-files))
      (cons "id" (map base-stem count-files)))))

(defn write-combined-count-file
  "combines *.counts files from a directory into a single file"
  ([count-dir out-file]
     (save (combine-count-files count-dir) out-file :delim "\t")
     out-file)
  ([count-dir]
     (write-combined-count-file count-dir
                                (io/file count-dir "combined.counts"))))


;; tests

(def test-count-file (get-resource "htseq-count/KO_Chol_rep1.counts"))
(def count-dir (get-resource "htseq-count"))

(fact
 "load-htseq loads a htseq-count file into an incanter.core.Dataset"
 (type (load-counts test-count-file)) => incanter.core.Dataset)

(fact
 "write-combined-count-file is functional"
 (let [out-file (io/file count-dir "combined.counts")]
   (write-combined-count-file count-dir) => out-file))
;   (io/delete-file out-file)))
