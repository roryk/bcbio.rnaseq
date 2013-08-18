(ns bcbio.rnaseq.core
  (:use [clostache.parser :only [render]]
        [clojure.java.shell :only [sh]]
        [incanter.io :only [read-dataset]]
        [incanter.core]
        [clojure.math.combinatorics :only [combinations]])
  (:require [clj-yaml.core :as yaml]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]))


;; (def config-file "/Users/rory/cache/bcbio.rnaseq/resources/bcbio_sample.yaml")
;;(def config (get-config config-file))

(defn dirname [path]
  (str (fs/parent (fs/expand-home path)) "/"))

(defn get-resource [filename]
  (.getFile (io/resource filename)))

(defn get-metadata [config]
  (map :metadata (:details config)))

(defn get-condition [config]
  (map :condition (get-metadata config)))

(def resource-dir (dirname (get-resource "edgeR.template")))
(def templates (fs/glob (str resource-dir "*.template")))

(defn generic-analysis-config [config]
  "create an analysis config from a parsed bcbio-nextgen sample file"
  (let [conditions (get-condition config)]
    {:count-file (:count-file config)
     :comparison (distinct conditions)
     :conditions conditions
     :condition-name (clojure.string/join "_vs_" (distinct conditions))}))


(defn get-config [config-file]
  (assoc (yaml/parse-string (slurp config-file)) :count-file
         (str (dirname config-file) "htseq-count/combined.counts")))

(defn get-descriptions [config]
  "get descriptions of samples from a parsed YAML sample file"
  (map :description (:details config)))




(defn- construct-htseq-filenames [base-dir config]
  (map #(str base-dir "/htseq-count/" % ".counts") (get-descriptions config)))

(defn load-htseq [htseq-file]
  (read-dataset htseq-file :delim \tab))

(defn htseq-files [config-file]
  (let [config (get-config config-file)
        base-dir (fs/parent config-file)]
    (construct-htseq-filenames base-dir config)))

(defn extract-count-column [htseq-file]
  (to-dataset (sel (load-htseq htseq-file) :cols 1)))

(defn merge-htseq-counts [combined htseq-file]
  (conj-cols combined (extract-count-column htseq-file)))

(defn base-filename [filename]
  (fs/split-ext (fs/base-name filename)))

(defn base-stem [filename]
  "get the stem of the base of the filename. (base-stem '/your/path/file.txt) => file"
  (first (base-filename filename)))

(defn combine-count-files [count-files]
  "combines a list of htseq-count files into a single file"
  (col-names
   (reduce merge-htseq-counts (load-htseq (first count-files))
           (rest count-files))
   (cons "id" (map base-stem count-files))))

(defn write-combined-count-file [count-files]
  (let [out-file (str (fs/parent (first count-files)) "/combined.counts")]
    (save (combine-count-files count-files) out-file :delim "\t")
    out-file))

(defn escape-quote [string]
  (str "\"" string "\""))

(defn seq-to-R-list [xs]
  "(seq-to-R-list [1, 2, 3]) => 'c(1, 2, 3)'"
  (str "c(" (clojure.string/join "," (map escape-quote xs)) ")"))




(defn change-extension [path extension]
  (str (dirname path) (base-stem path) extension))


(defn temporary-R-file [path]
  (fs/temp-file (name path) ".R"))

(defn write-template
  "writes an R file out from a template and returns the template name"
  [template count-file class out-file]
    (spit (change-extension template ".R")
          (render (slurp template) {:count-file (escape-quote count-file)
                                    :class class
                                    :out-file (escape-quote out-file)}))
    (change-extension template ".R"))

(defn generate-output-filename [f1 f2 analysis]
  (str f1 "_vs_" f2 "_" analysis ".tsv"))

(defn run-template
  ([template count-file class out-file]
     (sh "Rscript" (write-template template count-file class out-file))
     (change-extension template ".R"))
  ([template analysis-config]
     (run-template template (:count-file analysis-config)
                   (seq-to-R-list (:conditions analysis-config))
                   (:out-file analysis-config))))

(defn analysis-out-file [template-file analysis-config]
  (str (base-stem template-file) "_"
       (:condition-name analysis-config) ".tsv"))

(defn get-analysis-fn [config]
  "get a function that will run an analysis on a template file"
  (let [analysis-config (generic-analysis-config config)]
    (fn [template-file]
      (run-template template-file
                    (assoc analysis-config :out-file (analysis-out-file
                                                      template-file
                                                      analysis-config))))))
