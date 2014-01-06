(ns bcbio.rnaseq.compare
  (:use [bcbio.rnaseq.util]
        [bcbio.rnaseq.config]
        [clostache.parser :only [render-resource]]
        [clojure.java.shell :only [sh]]))


(defn write-template [template hashmap]
  (let [rfile (change-extension (swap-directory template (analysis-dir)) ".R")]
    (spit rfile (render-resource template hashmap))
  rfile))


(defn make-fc-plot [in-files]
  "create a fold change plot comparing to the seqc qPCR data"
  (let [template-file "comparisons/qPCR_foldchange.template"
        out-file (swap-directory "fc-plot.pdf" (analysis-dir))
        qpcr-file (get-resource "seqc/qPCR/qpcr_HBRR_vs_UHRR.tidy")
        template-config {:out-file (escape-quote out-file)
                         :qpcr-file (escape-quote qpcr-file)
                         :in-files (seq-to-rlist in-files)}]
    (apply sh ["Rscript" (write-template template-file template-config)])
    out-file))

