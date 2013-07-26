(ns bcbio.rnaseq
  (:use [clostache.parser :only [render]]
        [clojure.java.shell :only [sh]]
        [incanter.io :only [read-dataset]]
        [incanter.core])
  (:require [clj-yaml.core :as yaml]
            [me.raynes.fs :as fs]))

(spit "/Users/rory/cache/bcbio.rnaseq/resources/edgeR.R"
 (render (slurp "/Users/rory/cache/bcbio.rnaseq/resources/edgeR.template")
  {:count_file "test_count"
   :control_name "control_count"
   :experiment_name "experiment"
   :group "group"
   :out_file "out_file"}))

(sh "Rscript" "/Users/rory/cache/bcbio.rnaseq/resources/edgeR.template")

(def config-file "/Users/rory/cache/bcbio.rnaseq/resources/bcbio_sample.yaml")
(def config (yaml/parse-string (slurp config-file)))

(defn get-config [config-file]
  (yaml/parse-string (slurp config-file)))
(defn- get-bcbio-samples [config]
    (map :description (:details config)))

(defn- construct-htseq-filenames [base-dir config]
  (map #(str base-dir "/htseq-count/" % ".counts") (get-bcbio-samples config)))

(defn load-htseq [htseq-file]
  (read-dataset htseq-file :delim \tab))

(defn htseq-files [config-file]
  (let [config (get-config config-file)
        base-dir (fs/parent config-file)]
    (construct-htseq-filenames base-dir config)))

(def count-files (htseq-files config-file))

(defn extract-count-column [htseq-file]
  (to-dataset (sel (load-htseq htseq-file) :cols 1)))

(defn merge-htseq-counts [combined htseq-file]
  (conj-cols combined (extract-count-column htseq-file)))

(defn base-filename [filename]
  (fs/split-ext (fs/base-name filename)))

(defn base-stem [filename]
  (first (base-filename filename)))

(defn combine-count-files [config-file]
;  (let (out-file (str (fs/parent config-file) "/htseq-count/combined.counts"))
  (col-names
   (reduce merge-htseq-counts (load-htseq (first count-files))
           (rest count-files))
   (cons "id" (map base-stem count-files))))