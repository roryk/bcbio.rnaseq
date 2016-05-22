(ns bcbio.qcsummary.pathway
  (:require [bcbio.rnaseq.util :as util]
            [bcbio.qcsummary.r :refer [write-template]]))

(defrecord OrgInfo [name db symbol biomart-dataset])
(def org-dbs {:mouse (OrgInfo. "mouse" "org.Mm.eg.db" "mgi_symbol"
                               "mmusculus_gene_ensembl")
              :human (OrgInfo. "human" "org.Hs.eg.db" "hgnc_symbol"
                               "hsapiens_gene_ensembl")})

(def pathway-template "bcbio/pathway.template")

(defn write-pathway-template [out-dir organism]
  (let [pathway-config
        {:orgdb (util/escape-quote (-> org-dbs organism :db))
         :biomart-dataset (util/escape-quote
                           (-> org-dbs organism :biomart-dataset))}]
    (write-template pathway-template pathway-config out-dir ".pathway")))
