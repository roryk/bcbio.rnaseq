(ns bcbio.rnaseq.templates
  (:use [clostache.parser :only [render-resource]]
        [bcbio.rnaseq.config]
        [bcbio.rnaseq.util]
        [clojure.java.shell :only [sh]])
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(def templates ["templates/deseq.template", "templates/deseq2.template",
                "templates/edgeR.template", "templates/baySeq.template"])
(def caller-comparison-template "comparisons/compare.template")


(defn- analysis-out-stem [template-file analysis-config]
  (str (io/file (:de-out-dir analysis-config)
                (str (base-stem template-file) "_"
                     (:condition-name analysis-config)))))

(defn analysis-with-suffix [template-file analysis-config suffix]
  (str (analysis-out-stem template-file analysis-config) suffix))

(defn analysis-out-file [template-file analysis-config]
  (let [out-dir (:de-out-dir analysis-config)
        out-stem (analysis-out-stem template-file analysis-config)]
    (str (io/file out-dir out-stem ".tsv"))))

(defn normalized-count-file [template-file analysis-config]
  (let [out-dir (:de-out-dir analysis-config)
        out-stem (analysis-out-stem template-file analysis-config)]
    (str (io/file out-dir out-stem ".counts"))))

(defn write-template [template config]
  "writes an R file out from a template"
  (let [count-file (:count-file config)
        comparison (seq-to-factor (:conditions config))
        out-file (:out-file config)
        rfile (:r-file config)]
    (spit rfile
          (render-resource template {:count-file (escape-quote count-file)
                                    :class comparison
                                    :out-file (escape-quote out-file)
                                    :project (escape-quote (project-name))}))
    config))


(defn add-out-files-to-config [template analysis-config]
  (assoc analysis-config
    :out-file (analysis-with-suffix template analysis-config ".tsv")
    :normalized-file (analysis-with-suffix template analysis-config ".counts")
    :r-file (analysis-with-suffix template analysis-config ".R")))


(defn run-template [template analysis-config]
  (let [config (add-out-files-to-config template analysis-config)]
    (safe-makedir (:de-out-dir analysis-config))
    (write-template template config)
    (sh "Rscript" (:r-file config))
    config))

(defn get-analysis-config [key]
  "create an analysis config from a parsed bcbio-nextgen sample file"
  {:de-out-dir (analysis-dir)
   :count-file (combined-count-file)
   :comparison (distinct (metadata-key key))
   :conditions (metadata-key key)
   :condition-name (comparison-name key)})

(defn get-analysis-fn [key]
  "get a function that will run an analysis on a template file"
    (fn [template-file]
      (run-template template-file (get-analysis-config key))))
