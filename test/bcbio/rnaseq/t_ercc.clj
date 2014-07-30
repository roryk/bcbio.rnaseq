(ns bcbio.rnaseq.t-ercc
  (:require
   [bcbio.rnaseq.t-ercc :refer :all]
   [me.raynes.fs :as fs]
   [bcbio.rnaseq.util :refer [file-exists? get-resource safe-makedir]]
   [bcbio.rnaseq.ercc :as ercc]
   [bcbio.rnaseq.config :refer [analysis-dir]]
   [bcbio.rnaseq.test-setup :refer [test-setup default-bcbio-project]]
   [clojure.test :refer :all]))

(use-fixtures :once test-setup)

(deftest ERCC-analysis-works
  (let [in-files (map str (fs/glob (fs/file (get-resource
                                             "test-analysis/ercc/") "*_vs_*.tsv")))
        out-dir "test-output"]
    (safe-makedir out-dir)
    (is (file-exists? (ercc/ercc-analysis in-files out-dir)))
    (fs/delete-dir out-dir)))
