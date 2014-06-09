(ns bcbio.rnaseq.t-ercc
  (:require
   [bcbio.rnaseq.t-ercc :refer :all]
   [me.raynes.fs :as fs]
   [bcbio.rnaseq.util :refer [file-exists?]
   [bcbio.rnaseq.ercc: :as ercc]
   [bcbio.rnaseq.config :refer [analysis-dir]]
   [bcbio.rnaseq.test-setup :refer [test-setup default-bcbio-project]]
   [clojure.test :refer :all]))

(use-fixtures :once test-setup)

(deftest ERCC-analysis-works
  (let [in-files (map str (fs/glob (fs/file (analysis-dir) "*_vs_*.tsv")))]
    (is (util/file-exists? (ercc/ercc-analysis in-files)))))
