(ns bcbio.rnaseq.compare
  (:use [bcbio.rnaseq.util]
        [clostache.parser :only [render]]
        [clojure.java.shell :only [sh]]))

(defn write-template [template out-dir hashmap]
  (let [rfile (change-extension (swap-directory template out-dir) ".R")]
    (spit rfile (render (slurp template) hashmap))
  rfile))


(defn make-fc-plot [analysis-dir]
  (let [template-file (get-resource "comparisons/qPCR_foldchange.template")
        out-file (swap-directory "fc-plot.pdf" analysis-dir)
        qpcr-file (get-resource "seqc/qpcr/foldchange_qpcr.tidy")
        template-config {:out-file (escape-quote out-file)
                         :qpcr-file (escape-quote qpcr-file)
                         :analysis-dir (escape-quote analysis-dir)}]
    (apply sh ["Rscript" write-template template-file analysis-dir template-config])
    out-file))




