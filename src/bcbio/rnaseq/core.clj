(ns bcbio.rnaseq.core
  (:require [bcbio.qcsummary.core :as qcsummary]
            [bcbio.rnaseq.compare :as compare]
            [bcbio.rnaseq.simulate :as simulate]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class :main true))

(defn version
  "lookup version, snagged from bcbio.variation.recall"
  []
  (with-open [reader
              (-> "META-INF/maven/bcbio.rnaseq/bcbio.rnaseq/pom.properties"
                  io/resource
                  io/reader)]
    (-> (doto (java.util.Properties.)
          (.load reader))
        (.getProperty "version"))))

(def cli-options
  [["-h" "--help"]
   ["-v" "--version"]
   ["-n" "--cores CORES" "Number of cores"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(> % 0)]]
   [nil "--simulate" "Run on simulated data"]
   [nil "--counts-only" "Only run count-based analyses"]
   [nil "--seqc" "Data is from a SEQC alignment"]])

(defn usage [options-summary]
  (->> [
    ""
    "Usage: bcbio-rnaseq [options] bcbio-project-file"
    ""
    "Options:"
    options-summary
    ]
    (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))


(def subcommands
  [{:name "compare"
    :description "compare DE callers on the results of a bcbio-nextgen run"
    :entry compare/compare-cli}
   {:name "simulate"
    :description "compare DE callers using simulated data"
    :entry simulate/simulate-cli}
   {:name "summarize"
    :description "summarize the QC data from a bcbio-nextgen run"
    :entry qcsummary/summarize-cli}
   ])

(defn subcommand-to-string [{:keys [name description]}]
  (str "  " name ": " description \newline))

(defn subcommand-help [cmds]
  (string/join \newline
   [""
    "Usage: bcbio-rnaseq [subcommand]"
    ""
    "Subcommands"
    "-----------"
    (apply str (map subcommand-to-string cmds))
    ""
    "You can get more information about each subcommand"
    "e.g.: bcbio-rnaseq simulate --help"
    ]))

(defn has-subcommand? [cmd]
  (contains? (set (map :name subcommands)) cmd))

(defn cli-entry [cmd]
  (:entry (first (filter #(= (:name %) cmd) subcommands))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options
                                                               :in-order true)
        cmd (first arguments)]
    (cond
      (:version options) (exit 0 (str "bcbio.rnaseq: " (version)))
      (:help options) (exit 0 (subcommand-help subcommands))
      (not (has-subcommand? cmd)) (exit 0 (subcommand-help subcommands)))
    (apply (cli-entry cmd) (rest arguments))))
