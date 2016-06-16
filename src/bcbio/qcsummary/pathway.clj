(ns bcbio.qcsummary.pathway
  (:require [bcbio.rnaseq.util :as util]
            [bcbio.qcsummary.r :refer [write-template]]))

(defrecord OrgInfo [name db symbol biomart-dataset keggname])
(def org-dbs {:mouse (OrgInfo. "mouse" "org.Mm.eg.db" "mgi_symbol"
                               "mmusculus_gene_ensembl" "mmu")
              :human (OrgInfo. "human" "org.Hs.eg.db" "hgnc_symbol"
                               "hsapiens_gene_ensembl" "hsa")})

(def pathway-template "bcbio/pathway.template")

(defn write-pathway-template [out-dir organism]
  (let [pathway-config
        {:orgdb (util/escape-quote (-> org-dbs organism :db))
         :biomart-dataset (util/escape-quote
                           (-> org-dbs organism :biomart-dataset))
         :keggname (util/escape-quote
                    (-> org-dbs organism :keggname))}]
    (write-template pathway-template pathway-config out-dir ".pathway")))
