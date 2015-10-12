(ns bcbio.rnaseq.config
  (:require [bcbio.rnaseq.util :refer :all]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def cfg-state (atom {}))
(def get-config #(deref cfg-state))
(defn alter-config! [new-cfg]
  (swap! cfg-state (constantly new-cfg)))

(defn load-yaml [yaml-file]
  (yaml/parse-string (slurp yaml-file)))



(defn metadata-key [key]
  (map key (map :metadata (:samples (get-config)))))

(def get-description
  #(map :description (:samples (get-config))))

(defn program-path [prog]
  "query configuration for program path by keyword"
  (:cmd (prog (:resources (get-config))) (name prog)))

(def project-dir #(:project-dir (get-config)))
(def analysis-dir #(:analysis-dir (get-config)))
(def upload-dir #(:upload (get-config)))
(def sample-names #(map :description (:samples (get-config))))
(def genome-build #(first (map :genome_build (:samples (get-config)))))
(def ref-fasta #(:sam_ref (first (:samples (get-config)))))

(def gtf-file #(get-in (first (:samples (get-config)))
                       [:genome_resources :rnaseq :transcripts]))
(def library-type #(get-in (first (:samples (get-config)))
                           [:algorithm :strandedness] "unstranded"))
(defn count-files []
  (map #(str (fs/file (upload-dir) % (str % "-ready.counts"))) (sample-names)))

(defn comparison-name [key]
  (clojure.string/join "_vs_" (sort (distinct (metadata-key key)))))

(def combined-count-file
  #(str (fs/file (project-dir) "combined.counts")))

(def project-name
  #(:project-name (get-config)))

(defn setup-config [bcbio-project-file]
  (let [project-config (load-yaml bcbio-project-file)
        system-config (load-yaml (:bcbio_system project-config))]
    (alter-config! (merge system-config project-config))
    (alter-config! (assoc (get-config) :project-dir
                          (str (io/file (dirname bcbio-project-file)))))
    (alter-config! (assoc (get-config) :analysis-dir
                          (str (io/file (project-dir) "de"))))
    (alter-config! (assoc (get-config) :project-name
                          (fs/base-name (dirname (dirname bcbio-project-file)))))))
