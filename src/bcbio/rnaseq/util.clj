(ns bcbio.rnaseq.util
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn escape-quote [string]
  (str "\"" string "\""))

(defn get-resource [filename]
  (.getFile (io/resource filename)))

(defn base-filename [filename]
  (fs/split-ext (fs/base-name filename)))

(defn base-stem [filename]
  "get the stem of the base of the filename. (base-stem '/your/path/file.txt) => file"
  (first (base-filename filename)))

(defn get-files-with-extension [dir ext]
  (filter #(.endsWith (.getName %) ext) (file-seq (io/file dir))))

(defn dirname [path]
  (str (fs/parent (fs/expand-home path)) "/"))

(defn change-extension [path extension]
  (str (dirname path) (base-stem path) extension))

(defn seq-to-factor [xs]
  "(seq-to-factor [1, 2, 3]) => 'c(1, 2, 3)'"
  (str "factor(c(" (clojure.string/join "," (map escape-quote xs))
       "), levels=c(" (clojure.string/join "," (map escape-quote
                                                    (vec (apply sorted-set xs)))) "))"))

(defn- directory-exists? [dir]
  (.isDirectory (io/file dir)))

(defn safe-makedir [dir]
  (when-not (directory-exists? dir)
    (.mkdir (io/file dir))))

