(ns bcbio.rnaseq.cufflinks
  (:use [bcbio.rnaseq.util]
        [bcbio.rnaseq.config]
        [bcbio.run.itx :only [check-run]]
        [clojure.java.shell :only [sh]]
        [incanter.io :only [read-dataset]]
        [incanter.core :only [to-dataset sel conj-cols col-names save nrow]]
        [incanter.stats :only [mean]])
  (:require [bcbio.run.itx :as itx]
            [me.raynes.fs :as fs]
            [clojure.string :as string]
            [incanter.core :as incanter]))


(defn- align-files []
  (map #(str (fs/file (upload-dir) % (str % "-ready.bam"))) (sample-names)))

(defn group-samples [key]
  (let
      [m (zipmap (align-files) (metadata-key key))]
    (group-by val m)))

(defn- group-details [key]
  (group-by #(get-in % [:metadata key]) (:samples (get-config))))

(defn- group-details-to-samples [m]
  (map :description (val m)))


(defn correction [bamfiles]
  (let [counts (map count bamfiles)]
    (if (= (apply max counts) 1)
      "blind"
      (if (> (apply min counts) 3)
        "per-condition"
        "pooled"))))


(defn update-values [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn fmap [f m]
  (into (empty m) (for [[k v] m] [k (f v)])))

(defn bam-by-key [key]
  (fmap keys (group-samples key)))

(defn bamfiles [key]
  (as-> (bam-by-key key) labels
        (vals labels)))

(defn labels-arg [key sep]
  (as-> (bam-by-key key) labels
        (keys labels)
        (string/join sep labels)))

(defn bamfile-arg [key]
  (string/join " " (map #(string/join "," %) (vals (bam-by-key key)))))

(defn run-cuffdiff [cores key]
  "Run a Cuffdiff commandline"
  (let [cuffdiff (program-path :cuffdiff)
        library-arg (str "fr-" (library-type))
        output-dir (str (fs/file (analysis-dir) "cuffdiff" (labels-arg key "_vs_")))]
    (fs/mkdirs output-dir)
    (check-run (string/join " "
                            [cuffdiff
                             "--output-dir" output-dir
                             "--labels" (labels-arg key ",")
                             "--num-threads" (str cores)
                             "--dispersion-method" (correction (bamfiles key))
                             "--library-norm-method" "quartile"
                             "--library-type" library-arg
                             "--quiet"
                             "--emit-count-tables"
                             "--no-update-check"
                             "--no-js-tests"
                             "--min-alignment-count" "0"
                             (gtf-file)
                             (bamfile-arg key)]) "tmp")
    (str (fs/file output-dir "gene_exp.diff"))))

(defn read-cufflinks [in-file]
  (read-dataset in-file :delim \tab :header true))

(defn mean-expression [df]
  (map mean (map vector (sel df :cols :value_1) (sel df :cols :value_2))))

(defn reformat-gene-info [df]
  (col-names
   (conj-cols (sel df :cols :gene_id)
              (mean-expression df)
              (sel df :cols 10)
              (sel df :cols :p_value)
              (sel df :cols :q_value)
              (repeat (nrow df) "cuffdiff"))
   ["id" "expr" "logFC" "pval" "padj" "algorithm"]))

(defn write-gene-info [in-file out-file]
  (let [info-df (reformat-gene-info (read-cufflinks in-file))]
    (save info-df out-file :delim "\t")
    out-file))
