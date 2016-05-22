(ns bcbio.qcsummary.core
  (:require [bcbio.rnaseq.config :as config]
            [bcbio.rnaseq.util :as util]
            [bcbio.qcsummary.r :refer [knit-file write-template]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [bcbio.qcsummary.pathway :refer [write-pathway-template org-dbs]]
            [bcbio.qcsummary.sleuth :refer [write-sleuth-template]]
            [incanter.core :as ic]
            [me.raynes.fs :as fs]))

(def summary-template "bcbio/qc-summary.template")
(def deseq2-de-template "bcbio/deseq2-de.template")
(def dexseq-template "bcbio/dexseq.template")

(defn dexseq-file [file-name]
  (#(get-in (first (:samples (config/load-yaml file-name)))
            [:genome_resources :rnaseq :dexseq])))

(defn write-dexseq-template [out-dir formula summary-csv dexseq-gff]
  (let [dexseq-config {:condition (last (util/tokenize-formula formula))
                       :gff-file (util/escape-quote dexseq-gff)
                       :summary-csv (util/escape-quote summary-csv)}]
    (println "Running DEXSeq, this will take a long time.")
    (write-template dexseq-template dexseq-config out-dir ".tmp")))

(defn write-de-template [out-dir formula]
  (let [de-config {:formula formula
                   :condition (util/escape-quote (last (util/tokenize-formula formula)))}]
    (println "Running DESeq2.")
    (write-template deseq2-de-template de-config out-dir ".tmp")))

(defn make-qc-summary [out-dir summary-csv]
  (let [summary-config {:summary-csv (util/escape-quote summary-csv)
                        :out-dir (util/escape-quote out-dir)
                        :counts-file (->> "combined.counts"
                                          (fs/file (util/dirname summary-csv))
                                          str
                                          util/escape-quote)
                        :tx2genes (->> "tx2gene.csv"
                                       (fs/file (util/dirname summary-csv))
                                       str
                                       util/escape-quote)}]
    (write-template summary-template summary-config out-dir ".tmp")))


(defn load-summary [fn]
  (config/load-yaml fn))

(defn summary [sample] (get-in sample [:summary :metrics]))

(defn metadata [sample] (:metadata sample))

(defn tidy-summary [summary]
  "tidy a set of summary statistics"
  (if (every? empty? summary)
    summary
    (do
      (let [df (ic/to-dataset summary)]
        (ic/col-names df (map name (ic/col-names df)))))))

(defn load-tidy-summary [fn]
  "load the summaries from a bcbio project file"
  (let [summary (->> fn load-summary :samples (map summary) tidy-summary)
        metadata (->> fn load-summary :samples (map metadata) util/fix-missing-keys
                      tidy-summary)
        description (->> fn load-summary :samples (map :description) tidy-summary)]
    (ic/conj-cols (ic/col-names description [:description])
                  summary metadata)))

(defn write-tidy-summary [fn]
  "from a bcbio project file write a tidy version of the
   summary data as a CSV file"
  (let [out-dir (util/dirname fn)
        out-file (-> fn fs/split-ext first (str ".csv"))
        out-path (-> out-dir (io/file out-file) (.getPath))]
    (ic/save (load-tidy-summary fn) out-path)
    out-path))

(defn usage [options-summary]
  (->> [
        ""
        "Usage: bcbio-rnaseq summarize bcbio-project-file.yaml"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(def valid-organism-message
  (str "organism ("
       (->> org-dbs keys (map name) (string/join ", "))
       ")"))

(def invalid-organism-message
  (str "Organism must be one of "
       (->> org-dbs keys (map name) (string/join ", "))))

(def organism-validator
  [#(contains? (->> org-dbs keys set) %) invalid-organism-message])

(def options
  [["-h" "--help"]
   ["-f" "--formula FORMULA" "Formula to use in model (example: ~ batch + condition)"
    :default nil]
   ["-d" "--dexseq" "Run DEXSeq"]
   ["-o" "--organism ORGANISM" valid-organism-message
    :parse-fn #(if (keyword? %)
                 %
                 (-> % string/lower-case keyword))
    :default nil
    :validate [#(contains? (->> org-dbs keys set) %) invalid-organism-message]]
   ["-s" "--sleuth" "Run Sleuth"]])


(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn summarize [project-file options]
  (let [out-dir (-> project-file util/dirname (io/file "summary")
                    str util/safe-makedir)
        tidy-summary (write-tidy-summary project-file)
        qc-file (make-qc-summary out-dir tidy-summary)
        out-file (util/change-extension qc-file ".Rmd")
        dexseq-gff (dexseq-file project-file)]
    (util/catto out-file qc-file)
    (when-let [formula (:formula options)]
      (let [de-file (write-de-template out-dir formula)]
        (spit out-file (slurp de-file) :append true)
        (when (:dexseq options)
          (let [dexseq-out (write-dexseq-template out-dir formula
                                                  tidy-summary dexseq-gff)]
            (println "Setting up DEXSeq template.")
            (spit out-file (slurp dexseq-out) :append true)))
        (when (:sleuth options)
          (let [sleuth-out (write-sleuth-template out-dir)]
            (println "Setting up Sleuth template.")
            (spit out-file (slurp sleuth-out) :append true)))
        (when (:organism options)
          (let [pathway-out (write-pathway-template out-dir
                                                    (:organism options))]
            (println "Setting up pathway analysis template.")
            (spit out-file (slurp pathway-out) :append true)))))
    out-file))

(defn summarize-cli [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args options)]
    (cond
      (:help options) (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    (let [html-file (knit-file (summarize (first arguments) options))]
      (println "Summary report can be found here" html-file))))
