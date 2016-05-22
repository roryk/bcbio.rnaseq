(ns bcbio.qcsummary.pathway
  (:require [bcbio.rnaseq.util :as util]
            [bcbio.qcsummary.r :refer [write-template]]))

(defrecord OrgInfo [name db symbol])
(def org-dbs {:mouse (OrgInfo. "mouse" "org.Mm.eg.db" "mgi_symbol")
              :human (OrgInfo. "human" "org.Hs.eg.db" "hgnc_symbol")})

(def pathway-template "bcbio/pathway.template")

(defn write-pathway-template [out-dir organism]
  (let [pathway-config {:orgdb (util/escape-quote (-> org-dbs organism :db))}]
    (write-template pathway-template pathway-config out-dir ".pathway")))
