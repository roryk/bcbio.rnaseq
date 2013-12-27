(ns bcbio.rnaseq.t-core
  (:use
   [midje.sweet]
   [bcbio.rnaseq.util]
   [bcbio.rnaseq.htseq-combine :only [load-counts write-combined-count-file]]
   [bcbio.rnaseq.templates :only [templates get-analysis-config run-template]]
   [bcbio.rnaseq.config]
   [bcbio.rnaseq.compare :only [make-fc-plot]]
   [bcbio.rnaseq.cufflinks :only [run-cuffdiff]]
   [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [bcbio.rnaseq.core :as core]))

(def bcbio-rnaseq-jar
  "/v-data/bcbio.rnaseq/target/bcbio.rnaseq-0.0.1-SNAPSHOT-standalone.jar")

(defn this-jar
  "utility function to get the name of jar in which this function is invoked"
  [& [ns]]
  (-> (or ns (class *ns*))
      .getProtectionDomain .getCodeSource .getLocation .getPath))

(setup-config default-bcbio-project)

(facts
 "facts about template files"
 (fact
  "running a single template file is functional"
  (let [template (first templates)
        analysis-config (get-analysis-config :panel)]
    (write-combined-count-file (count-files) (combined-count-file))
    (file-exists? (:out-file (run-template template analysis-config))) => true))
 (fact
  "running a group of analyses produces output files"
  (every? file-exists? (map :out-file (core/run-R-analyses :panel))) => true)
 (fact
  "making the comparison plot automatically works"
  (let [in-files (map str (fs/glob (fs/file (analysis-dir) "*.tsv")))]
    (file-exists? (make-fc-plot in-files)) => true)))

(facts
 "facts about cufflinks"
  (fact
  "running Cuffdiff works"
  (file-exists? (:out-file (core/run-cuffdiff :panel))) => true))

(fact
 "combining R analyses and cuffdiff works"
 (file-exists? (core/run-comparisons :panel)) => true)

(fact
 "making the seqc plots work"
 (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
       in-files (fs/glob (str dirname "*.tsv"))]
   (alter-config! (assoc (get-config) :analysis-dir dirname))
   (file-exists? (make-fc-plot in-files)) => true))

(fact
 "making comparison plots from a project works"
 (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
       in-files (fs/glob (str dirname "*.tsv"))]
   (file-exists? (:fc-plot (core/compare-callers in-files))) => true))

(fact
 "running the comparisons on a bcbio-nextgen project file works"
 (let [out-map (core/main "compare-bcbio-run"
                          default-bcbio-project "panel")]
   (file-exists? (:fc-plot out-map)) => true))
