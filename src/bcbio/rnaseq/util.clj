(ns bcbio.rnaseq.util
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn swap-directory [file dir]
  "/path/to/file -> /dir/file"
  (str (io/file dir (fs/base-name file))))

(defn escape-quote [string]
  (str "\"" string "\""))

(defn get-resource [filename]
  (.getFile (io/resource filename)))

(defn base-filename [filename]
  (fs/split-ext (fs/base-name filename)))

(defn base-stem [filename]
  "get the stem of the base of the filename. (base-stem '/your/path/file.txt) => file"
  (first (base-filename filename)))


(defn get-files-in-directory [directory]
  (->> directory
       clojure.java.io/file
       .listFiles
       (filter #(.isFile %))
       (map #(str %))))

(defn has-file-extension? [file-name extension]
  (.endsWith file-name extension))

(defn filter-on-extension [files extension]
  (filter #(has-file-extension? % extension) files))

(defn get-files-with-extension [directory extension]
  (filter-on-extension (get-files-in-directory directory) extension))

(defn dirname [path]
  (str (fs/parent (fs/expand-home path)) "/"))

(defn change-extension [path extension]
  (str (dirname path) (base-stem path) extension))

(defn seq-to-factor [xs]
  "(seq-to-factor [1, 2, 3]) => 'factor(c(1, 2, 3), levels=c(1,2,3))'"
  (str "factor(c(" (clojure.string/join "," (map escape-quote xs))
       "), levels=c(" (clojure.string/join "," (map escape-quote
                                                    (vec (apply sorted-set xs)))) "))"))
(defn seq-to-rlist [xs]
  "(seq-to-rlist [1, 2, 3]) => 'c(1, 2, 3)'"
  (str "c(" (clojure.string/join "," (map escape-quote xs)) ")"))

(defn- directory-exists? [dir]
  (.isDirectory (io/file dir)))

(defn safe-makedir [dir]
  (when-not (directory-exists? dir)
    (.mkdir (io/file dir))))

(defn file-exists? [fname]
  (.exists (io/as-file fname)))

(defn apply-to-keys [m f & keyseq]
  (reduce #(assoc %1 %2 (f (%1 %2))) m keyseq))
