(ns bcbio.rnaseq.core
    (:use [clostache.parser :only [render]]
        [clojure.java.shell :only [sh]]
        [incanter.io :only [read-dataset]]
        [me.raynes.fs]
        [clojure.string :only [join]]))

(defn dirname [path]
  (str (parent (expand-home test-path)) "/"))

(defn change-extension [path extension]
  (str (dirname path) (name path) extension))

(defn temporary-R-file [path]
  (temp-file (name path) ".R"))

(defn seq-to-R-list [xs]
  " this needs to take a vec and turn it into c(v[0], v[1], v[2], etc)"
  (str "c(" (join "," xs) ")"))



(defn write-template
  "writes an R file out from a template and returns the template name"
  [template count_file class]
  (do
    (spit (change-extension template ".R")
          (render (slurp template) {:count_file count_file :class class}))
    (change-extension template ".R")))

(defn run-template [template count_file class]
  (sh "Rscript" (write-template template count_file class)))