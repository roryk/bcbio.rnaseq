(ns bcbio.rnaseq.htseq-combine
  (:use [incanter.io :only [read-dataset]]
        [incanter.core :only [to-dataset sel conj-cols col-names save nrow]]
        [bcbio.rnaseq.util]
        [clojure.tools.cli :only [cli]])
  (:require [clojure.java.io :as io]))

(defn- get-count-files [count-dir]
  (get-files-with-extension count-dir ".counts"))

(defn load-counts [count-file]
  "load a tab delimited count-file with the first column as the identifier"
  (read-dataset count-file :delim \tab))

(defn- extract-count-column [count-file]
  (to-dataset (sel (load-counts count-file) :cols 1)))

(defn- merge-counts [combined count-file]
  (conj-cols combined (extract-count-column count-file)))

(defn combine-count-dir [count-dir]
  "loads *.counts files in a directory into a single incanter Dataset object"
  (let [count-files (get-count-files count-dir)]
     (col-names
      (reduce merge-counts (load-counts (first count-files)) (rest count-files))
      (cons "id" (map base-stem count-files)))))

(defn combine-count-files [count-files]
  (col-names
   (reduce merge-counts (load-counts (first count-files)) (rest count-files))
   (cons "id" (map base-stem count-files))))

(defn drop-last-rows [dataset n]
  "removes the last n rows from a Incanter dataset"
  (sel dataset :except-rows (range (- (nrow dataset) 5) (nrow dataset))))

(defn write-combined-count-file
  "combines *.counts files from a directory into a single file"
  ([count-files out-file]
     (if (.exists (io/as-file out-file))
       out-file)
     (do
       (io/make-parents (io/as-file out-file))
       (save (combine-count-files count-files) out-file :delim "\t")
       out-file))
  ([count-files]
     (let [count-dir (dirname (first count-files))]
       (write-combined-count-file count-files
                                  (str (io/file count-dir "combined.counts"))))))

(defn write-combined-count-dir
  "combines *.counts files from a directory into a single file"
  ([count-dir out-file]
     (if (.exists (io/as-file out-file))
       out-file)
     (do
       (io/make-parents (io/as-file out-file))
         ; last 5 rows of htsseq-count datasets are summary rows
       (save (drop-last-rows (combine-count-dir count-dir) 5) out-file
               :delim "\t")
       out-file))
  ([count-dir]
     (write-combined-count-dir count-dir
                                (str (io/file count-dir "combined.counts")))))



(defn cl-entry [& args]
  (let [[opts [count-dir] banner]
        (cli args
             ["-h" "--help" "Show help" :flag true :default false]
             ["-o" "--out-file" "Output file name"])]
    (when (or (:help opts) (some nil? [count-dir]))
      (println "Required arguments:")
      (println "    <count-dir> directory of .counts files to combine")
      (println banner)
      (System/exit 0))
    (if (:out-file opts)
      (write-combined-count-dir count-dir (:out-file opts))
      (write-combined-count-dir count-dir))))
