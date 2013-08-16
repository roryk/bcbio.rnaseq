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

(defn- get-descriptions [config]
  (map :description (:details config)))

(defn get-metadata [config]
  (map :metadata (:details config)))

(defn get-condition [config]
  (map :condition (get-metadata config)))

(defn- construct-htseq-filenames [base-dir config]
  (map #(str base-dir "/htseq-count/" % ".counts") (get-descriptions config)))

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

(defn combine-count-files [count-files]
  (col-names
   (reduce merge-htseq-counts (load-htseq (first count-files))
           (rest count-files))
   (cons "id" (map base-stem count-files))))

(declare that-function-to-write)

(defn write-combined-count-file [count-files]
  (let [out-file (str (fs/parent (first count-files)) "/combined.counts")]
    (that-function-to-write (combine-count-files count-files) out-file)
    (out-file)))

(defn seq-to-R-list [xs]
  " this needs to take a vec and turn it into c(v[0], v[1], v[2], etc)"
  (str "c(" (clojure.string/join "," xs) ")"))


(defn dirname [path]
  (str (parent (expand-home test-path)) "/"))

(defn change-extension [path extension]
  (str (dirname path) (name path) extension))

(defn temporary-R-file [path]
  (temp-file (name path) ".R"))

(defn write-template
  "writes an R file out from a template and returns the template name"
  [template count_file class]
  (do
    (spit (change-extension template ".R")
          (render (slurp template) {:count_file count_file :class class}))
    (change-extension template ".R")))

(defn run-template [template count_file class]
  (sh "Rscript" (write-template template count_file class)))