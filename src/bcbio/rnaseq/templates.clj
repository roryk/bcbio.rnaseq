(ns bcbio.rnaseq.templates
  (:use [clostache.parser :only [render]]
        [bcbio.rnaseq.config :only [get-analysis-config parse-bcbio-config]]
        [bcbio.rnaseq.util]
        [clojure.java.shell :only [sh]])
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(def resource-dir (dirname (get-resource "edgeR.template")))
(def templates (fs/glob (str resource-dir "*.template")))

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
          (render (slurp template) {:count-file (escape-quote count-file)
                                    :class comparison
                                    :out-file (escape-quote out-file)}))
    config))


(defn add-out-files-to-config [template analysis-config]
  (assoc analysis-config
    :out-file (analysis-with-suffix template analysis-config ".tsv")
    :normalized-file (analysis-with-suffix template analysis-config ".counts")
    :r-file (analysis-with-suffix template analysis-config ".R")))


(defn run-template [template analysis-config]
  (let [config (add-out-files-to-config template analysis-config)]
    (write-template template config)
    (sh "Rscript" (:r-file config))
    config))

(defn get-analysis-fn [config]
  "get a function that will run an analysis on a template file"
  (let [analysis-config (get-analysis-config config)]
    (fn [template-file]
      (run-template template-file analysis-config))))
