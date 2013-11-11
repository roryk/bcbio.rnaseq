(ns bcbio.rnaseq.templates
  (:use [clostache.parser :only [render]]
        [midje.sweet]
        [bcbio.rnaseq.config :only [get-analysis-config parse-bcbio-config]]
        [bcbio.rnaseq.util]
        [clojure.java.shell :only [sh]])
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(def resource-dir (dirname (get-resource "edgeR.template")))
(def templates (fs/glob (str resource-dir "*.template")))

(defn write-template
  "writes an R file out from a template"
  [template count-file class out-file]
  (let [base-rfile (fs/base-name (change-extension template ".R"))
        rfile (str (io/file (dirname out-file) base-rfile))]
    (spit rfile
          (render (slurp template) {:count-file (escape-quote count-file)
                                    :class class
                                    :out-file (escape-quote out-file)}))
    rfile))

(defn run-template
  ([template count-file class out-file]
     (let [template-file (write-template template count-file class out-file)]
       (sh "Rscript" template-file)
       template-file))
  ([template analysis-config]
     (run-template template (:count-file analysis-config)
                   (seq-to-factor (:conditions analysis-config))
                   (:out-file analysis-config))
     analysis-config))

(defn analysis-out-file [template-file analysis-config]
  (let [out-dir (:de-out-dir analysis-config)
        out-base (str (base-stem template-file) "_"
                      (:condition-name analysis-config) ".tsv")]
    (str (io/file out-dir out-base))))

(defn get-analysis-fn [config]
  "get a function that will run an analysis on a template file"
  (let [analysis-config (get-analysis-config config)]
    (fn [template-file]
      (run-template template-file
                    (assoc analysis-config :out-file (analysis-out-file
                                                      template-file
                                                      analysis-config))))))

;; tests
(def test-config-file (get-resource "bcbio_sample.yaml"))
(def test-out-dir "de-analysis")
(def test-config (parse-bcbio-config test-config-file test-out-dir))
(def edger-template-file (get-resource "edgeR.template"))
(fact "get-analysis-fn returns a function to run an analysis"
      (:out-file ((get-analysis-fn test-config)
                  edger-template-file)) =>
                  (str (io/file test-out-dir "edgeR_control_vs_cholesterol.tsv")))
;                  (io/delete-file "edgeR_control_vs_cholesterol.tsv"))
