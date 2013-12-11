(ns bcbio.rnaseq.config
  (:use [bcbio.rnaseq.util])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(def default-bcbio-system (get-resource "bcbio_system.yaml"))
(def default-bcbio-sample (get-resource "bcbio_sample.yaml"))

(def cfg-state (atom {}))
(def get-config #(deref cfg-state))
(defn alter-config! [new-cfg]
  (swap! cfg-state (constantly new-cfg)))

(defn- load-yaml [yaml-file]
  (yaml/parse-string (slurp yaml-file)))

(defn parse-bcbio-config
  ([config-file out-dir]
     (safe-makedir out-dir)
     (assoc (load-yaml config-file)
       :count-file (str (io/file out-dir "combined.counts"))
       :de-out-dir out-dir))
  ([config-file]
     (parse-bcbio-config config-file "de-analysis")))

(defn- get-metadata [config]
  (map :metadata (:details config)))

(defn- get-condition [config]
  (map :condition (get-metadata config)))

(defn- get-description [config]
  (map :description (:details config)))

(defn metadata-key [key]
  (map key (map :metadata (:details (get-config)))))

(defn get-analysis-config [config]
  "create an analysis config from a parsed bcbio-nextgen sample file"
  (let [conditions (get-condition config)]
        {:de-out-dir (:de-out-dir config)
         :count-file (:count-file config)
         :comparison (distinct conditions)
         :conditions conditions
         :condition-name (clojure.string/join "_vs_" (distinct conditions))}))

(defn setup-config [bcbio-system bcbio-sample]
  (let [m (merge (load-yaml bcbio-system)
                 (load-yaml bcbio-sample))]
    (alter-config! (assoc m :galaxy-dir (dirname bcbio-system)))))

(defn program-path [prog]
  "query configuration for program path by keyword"
  (:cmd (prog (:resources (get-config))) (name prog)))

(def upload-dir #(get-in (get-config) [:upload :dir]))
(def sample-names #(map :description (:details (get-config))))
(def genome-build #(first (map :genome_build (:details (get-config)))))
(def ref-fasta (fs/file (:galaxy-dir (get-config))))
;;; look in tool-data/sam to find the reference fasta file
