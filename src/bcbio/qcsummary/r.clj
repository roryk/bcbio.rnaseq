(ns bcbio.qcsummary.r
  (:require [bcbio.rnaseq.util :as util]
            [clojure.java.shell :refer [sh]]
            [clostache.parser :as stache]))

(def install-libraries-message
  "There was an issue rendering the Rmarkdown file. You may need to install
   the R libraries (see https://github.com/roryk/bcbio.rnaseq) for instructions.
   If you have done that, there may be an issue with the Rmarkdown file.")

(defn run-rscript [rmd-file]
  (let [setwd (str "setwd('" (util/dirname rmd-file) "');")
        res (sh "Rscript" "-e"
                (str setwd "library(rmarkdown); render('" rmd-file "')"))]
    (println (:out res))
    res))

(defn knit-file [rmd-file]
  (let [out-file (util/change-extension rmd-file ".html")]
    (if (util/pandoc-supports-rmarkdown?)
      (let [res (run-rscript rmd-file)]
        (case (:exit res)
          0 out-file
          (do
            (println install-libraries-message)
            rmd-file)))
      rmd-file)))

(defn write-template [template hashmap out-dir extension]
  (let [rfile (util/change-extension (util/swap-directory template out-dir)
                                     extension)]
    (spit rfile (stache/render-resource template hashmap))
    rfile))
