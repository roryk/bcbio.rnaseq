(ns bcbio.rnaseq.t-core
  (:use
   [midje.sweet]
   [bcbio.rnaseq.util]
   [bcbio.rnaseq.htseq-combine :only [load-counts write-combined-count-file]]
   [bcbio.rnaseq.templates :only [templates get-analysis-config run-template
                                  run-R-analyses]]
   [bcbio.rnaseq.config]
   [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io]
            [bcbio.rnaseq.cuffdiff :as cuffdiff]
            [bcbio.rnaseq.compare :as compare]
            [bcbio.rnaseq.simulate :as simulate]
            [me.raynes.fs :as fs]
            [bcbio.rnaseq.core :as core]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [clj-yaml.core :as yaml]
            [clj-http.client :as client]
            [bcbio.rnaseq.ercc :as ercc]))

(def data-url
  "https://dl.dropboxusercontent.com/u/2822886/chb/bcbio.rnaseq/sample-project.tar")

(def stock-bcbio-project
  #(get-resource
    "seqc/sample-project/ERCC92/131111_standardization/project-summary-stock.yaml"))

(def default-bcbio-project
  #(str (fs/file
         (get-resource "seqc/sample-project/ERCC92/131111_standardization")
         "project-summary.yaml")))

(defn replace-if [pred s match replacement]
  (if (pred s)
    (string/replace s match replacement)
    s))

(defn replace-project-dir [config]
  (walk/prewalk
   #(replace-if string?
                %
                "/n/hsphS10/hsphfs1/chb/projects/bcbio-rnaseq/data/geo_data/standardization/ercc_subset/../ERCC92"
                (get-resource "seqc/sample-project/ERCC92"))
   config))

(defn replace-bcbio-system [config]
  (walk/prewalk
   #(replace-if string?
                %
                "/n/hsphS10/hsphfs1/chb/biodata/galaxy/bcbio_system.yaml"
                (get-resource "seqc/sample-project/bcbio_system.yaml"))
   config))

(defn replace-genome-dir [config]
  (walk/prewalk
   #(replace-if string?
                %
                "/n/hsphS10/hsphfs1/chb/biodata"
                (get-resource "seqc/sample-project"))
   config))

(defn fix-dirs [m]
  (-> m replace-project-dir replace-bcbio-system replace-genome-dir))

(defn fix-project-config []
  (when-not (file-exists? (default-bcbio-project))
    (let [config (load-yaml (stock-bcbio-project))]
      (spit (default-bcbio-project) (yaml/generate-string (fix-dirs config))))))

(defn download-file [url out-file]
  (when-not (file-exists? out-file)
    (with-open [out-buffer (io/output-stream out-file)]
      (.write out-buffer (:body (client/get url {:as :byte-array}))))
    out-file))

(defn install-test-data []
  (when-not (file-exists? (str (fs/file (get-resource "seqc") "sample-project")))
    (let [tar-file
          (str (fs/file (get-resource "seqc") (fs/base-name data-url)))]
      (println  (format "Downloading %s to %s." data-url tar-file))
      (download-file data-url tar-file)
      (sh "tar" "-xvf" tar-file "-C" (dirname tar-file)))))

(install-test-data)
(fix-project-config)
(setup-config (default-bcbio-project))

(facts :integration
 "facts about template files"
 (fact
  "running a single template file is functional"
  (let [template (first templates)
        analysis-config (get-analysis-config :panel)]
    (write-combined-count-file (count-files) (combined-count-file))
;    (:out-file (run-template template analysis-config)) => true))
    (file-exists? (:out-file (run-template template analysis-config))) => true))

 (fact
  "running a group of analyses produces output files"
  (every? file-exists? (map :out-file (run-R-analyses :panel))) => true)
 (fact
  "making the comparison plot automatically works"
  (let [in-files (map str (fs/glob (fs/file (analysis-dir) "*_vs_*.tsv")))]
    (file-exists? (compare/make-fc-plot in-files)) => true))
 (fact
  "ERCC analysis works"
  (let [in-files (map str (fs/glob (fs/file (analysis-dir) "*_vs_*.tsv")))]
    (file-exists? (ercc/ercc-analysis in-files)) => true)))

(facts :integration
 "facts about Cuffdiff"
  (fact
  "running Cuffdiff works"
  (file-exists? (:out-file (cuffdiff/run :panel 1))) => true))

(fact :integration
 "combining R analyses and cuffdiff works"
 (file-exists? (compare/run-comparisons :panel 1 false)) => true)

(fact :integration
 "making the seqc plots work"
 (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
       in-files (fs/glob (str dirname "*_vs_*.tsv"))]
   (alter-config! (assoc (get-config) :analysis-dir dirname))
   (file-exists? (compare/make-fc-plot in-files)) => true))

(fact :integration
 "making comparison plots from a project works"
 (let [dirname (dirname (get-resource "test-analysis/combined.counts"))
       in-files (fs/glob (str dirname "*_vs_*.tsv"))]
   (file-exists? (:fc-plot (compare/compare-callers in-files))) => true))

(fact :integration
 "running the comparisons on a bcbio-nextgen project file works"
 (let [out-map (core/-main "compare" (default-bcbio-project) "panel")]
   (file-exists? (:fc-plot out-map)) => true))

;;(alter-config! {})
(fact :integration
 "test running on simulated data"
 (file-exists? (simulate/run-simulation "simulate" 3)) => true)
 ;(every? file-exists? (core/run-simulation)) => true)

;; clean-up the analysis directory
;;(when (directory-exists? (get-resource "seqc/sample-project"))
;;  (fs/delete-dir (get-resource "seqc/sample-project")))
