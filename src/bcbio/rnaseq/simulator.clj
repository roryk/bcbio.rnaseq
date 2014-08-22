(ns bcbio.rnaseq.simulator
  (:require
   [bcbio.rnaseq.util :as util :refer
    [get-resource escape-quote swap-directory change-extension file-exists?]]
   [clostache.parser :refer [render-resource]]
   [clojure.java.shell :refer [sh]]
   [me.raynes.fs :as fs]))

(def simulate-template "comparisons/simulate.template")

(defn simulate-config [out-dir num-genes sample-size library-size count-file]
  {:input-file (if count-file (escape-quote count-file) "NA")
   :count-file (escape-quote (str (fs/file out-dir "sim.counts")))
   :num-genes num-genes
   :sample-size sample-size
   :library-size library-size})

(def default-count-file (util/get-resource "test-analysis/combined.counts"))

(defn write-simulation [out-dir num-genes sample-size library-size count-file]
  "write the simulate.R file from the default template"
  (let [sim-config (simulate-config out-dir num-genes sample-size library-size count-file)
        r-file (-> simulate-template (swap-directory out-dir) (change-extension ".R"))]
    (spit r-file (render-resource simulate-template sim-config))
    r-file))

(defn run-Rscript [r-file count-file]
  "run the simulation"
  (when-not (file-exists? count-file)
    (sh "Rscript" "--verbose" r-file))
  count-file)

(defn simulate [out-dir num-genes sample-size library-size input-file]
  (let [r-file (write-simulation out-dir num-genes sample-size library-size input-file)
        count-file (run-Rscript r-file (str (fs/file out-dir "sim.counts")))]
    (run-Rscript r-file count-file)))
