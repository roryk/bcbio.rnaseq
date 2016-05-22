(ns bcbio.qcsummary.sleuth
  (:require [bcbio.rnaseq.util :as util]
            [bcbio.qcsummary.r :refer [write-template]]))

(def sleuth-template "bcbio/sleuth.template")

(defn write-sleuth-template [out-dir]
  (let [sleuth-config {}]
    (write-template sleuth-template sleuth-config out-dir ".sleuth")))
