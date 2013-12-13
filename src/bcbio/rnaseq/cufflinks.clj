(ns bcbio.rnaseq.cufflinks
  (:use [bcbio.rnaseq.util]
        [bcbio.rnaseq.config]
        [clojure.java.shell :only [sh]])
  (:require [bcbio.run.itx :as itx]
            [me.raynes.fs :as fs]
            [clojure.string :as string]))


(defn- align-files []
  (map #(str (fs/file (upload-dir) (str % "-ready.bam"))) (sample-names)))

(defn group-samples [key]
  (let
      [m (zipmap (align-files) (metadata-key :condition))]
    (group-by val m)))

(defn- group-details [key]
  (group-by #(get-in % [:metadata key]) (:details (get-config))))

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
        output-dir (str (fs/file (upload-dir) "cufflinks" (labels-arg key "_vs_")))]
    (fs/mkdirs output-dir)
    (apply sh (flatten [cuffdiff
                      "--output-dir" output-dir
                      "--labels" (labels-arg key ",")
                      "--num-threads" (str cores)
                      "--dispersion-method" (correction (bamfiles key))
                      "--library-norm-method" "quartile"
                      "--library-type" library-arg
                      (gtf-file)
                      (bamfile-arg key)]))))
