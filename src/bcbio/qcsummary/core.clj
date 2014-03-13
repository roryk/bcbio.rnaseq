(ns bcbio.qcsummary.core
  (:require [bcbio.rnaseq.util :as util]
            [bcbio.rnaseq.config :as config]
            [incanter.core :as ic]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.string :as string])
  (:use [clojure.tools.cli :refer [parse-opts]]))

(defn load-summary [fn]
  (config/load-yaml fn))

(defn summary [sample] (get-in sample [:summary :metrics]))

(defn tidy-summary [summary]
  "tidy a set of summary statistics"
  (let [df (ic/to-dataset summary)]
    (ic/col-names df (map name (ic/col-names df)))))

(defn load-tidy-summary [fn]
  "load the summaries from a bcbio project file"
  (->> fn load-summary :samples (map summary) tidy-summary))

(defn write-tidy-summary [fn]
  "from a bcbio project file write a tidy version of the
   summary data as a CSV file"
  (let [out-dir (util/dirname fn)
        out-file (-> fn fs/split-ext first (str ".csv"))
        out-path (-> out-dir (io/file out-file) (.getPath))]
    (ic/save (load-tidy-summary fn) out-path)
    out-path))

(defn usage [options-summary]
  (->> [
        ""
        "Usage: bcbio-rnaseq summarize [options]"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(def options
  [["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn summarize-cli [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args options)]
    (cond
     (:help options) (exit 0 (usage summary)))
    (write-tidy-summary (first arguments))))
