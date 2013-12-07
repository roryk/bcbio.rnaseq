(ns bcbio.rnaseq.compare
  (:use [bcbio.rnaseq.util]
        [clostache.parser :only [render]]
        [clojure.java.shell :only [sh]]))

(defn write-template [template out-dir hashmap]
  (let [rfile (change-extension (swap-directory out-dir template) ".R")]
    (spit rfile (render (slurp template) hashmap))
  rfile))


(defn make-fc-plot [analysis-dir]
  (let [template-file (get-resource "comparisons/qPCR_foldchange.template")
        template-config {:out-file (swap-directory "fc-plot.pdf" analysis-dir)
                         :qpcr-file (get-resource "seqc/qPCR/foldchange_qpcr.tidy")
                         :analysis-dir analysis-dir}]
    (sh "Rscript" (write-template template-file analysis-dir template-config))
    (:out-file template-config)))




