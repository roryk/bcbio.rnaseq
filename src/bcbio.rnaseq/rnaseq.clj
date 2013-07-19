(ns bcbio.rnaseq
  (:use [clostache.parser :only [render]]
        [clojure.java.shell :only [sh]]
        [incanter.io :only [read-dataset]]))


(spit "/Users/rory/cache/bcbio.rnaseq/resources/edgeR.R"
 (render (slurp "/Users/rory/cache/bcbio.rnaseq/resources/edgeR.template")
  {:count_file "test_count"
   :control_name "control_count"
   :experiment_name "experiment"
   :group "group"
   :out_file "out_file"}))

(sh "Rscript" "/Users/rory/cache/bcbio.rnaseq/resources/edgeR.R")
