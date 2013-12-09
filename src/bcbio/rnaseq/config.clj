(ns bcbio.rnaseq.config
  (:use [bcbio.rnaseq.util])
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]))

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


(defn get-analysis-config [config]
  "create an analysis config from a parsed bcbio-nextgen sample file"
  (let [conditions (get-condition config)]
        {:de-out-dir (:de-out-dir config)
         :count-file (:count-file config)
         :comparison (distinct conditions)
         :conditions conditions
         :condition-name (clojure.string/join "_vs_" (distinct conditions))}))

(defn setup-config [bcbio-system bcbio-sample]
  (alter-config! (merge (load-yaml bcbio-system)
                        (load-yaml bcbio-sample)))

(defn program-path [prog]
  "query configuration for program path by keyword"
  (:cmd (prog (:resources (get-config))) (name prog)))
