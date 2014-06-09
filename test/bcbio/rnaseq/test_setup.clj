(ns bcbio.rnaseq.test-setup
  (:require
   [bcbio.rnaseq.test-setup :refer :all]
   [me.raynes.fs :as fs]
   [clojure.walk :as walk]
   [clojure.java.shell :refer [sh]]
   [bcbio.rnaseq.config :refer [load-yaml setup-config alter-config!]]
   [clj-yaml.core :refer [generate-string]]
   [clojure.java.io :as io]
   [clj-http.client :as client]
   [bcbio.rnaseq.util :refer [file-exists? get-resource replace-if dirname
                              directory-exists?]]))

(def data-url
  "https://dl.dropboxusercontent.com/u/2822886/chb/bcbio.rnaseq/sample-project.tar")

(def stock-bcbio-project
  #(get-resource
    "seqc/sample-project/ERCC92/131111_standardization/project-summary-stock.yaml"))

(def default-bcbio-project
  #(str (fs/file
         (get-resource "seqc/sample-project/ERCC92/131111_standardization")
         "project-summary.yaml")))

(defn replace-project-dir [config]
  (walk/prewalk
   #(replace-if string?
                %
                "/n/hsphS10/hsphfs1/chb/projects/bcbio-rnaseq/data/geo_data/standardization/ercc_subset/../ERCC92"
                (get-resource "seqc/sample-project/ERCC92"))
   config))

(defn replace-bcbio-system [config]
  (walk/prewalk
   #(replace-if string?
                %
                "/n/hsphS10/hsphfs1/chb/biodata/galaxy/bcbio_system.yaml"
                (get-resource "seqc/sample-project/bcbio_system.yaml"))
   config))

(defn replace-genome-dir [config]
  (walk/prewalk
   #(replace-if string?
                %
                "/n/hsphS10/hsphfs1/chb/biodata"
                (get-resource "seqc/sample-project"))
   config))


(defn fix-dirs [m]
  (-> m replace-project-dir replace-bcbio-system replace-genome-dir))

(defn fix-project-config []
  (when-not (file-exists? (default-bcbio-project))
    (let [config (load-yaml (stock-bcbio-project))]
      (spit (default-bcbio-project) (generate-string (fix-dirs config))))))

(defn download-file [url out-file]
  (when-not (file-exists? out-file)
    (with-open [out-buffer (io/output-stream out-file)]
      (.write out-buffer (:body (client/get url {:as :byte-array}))))
    out-file))

(defn install-test-data []
  (when-not (directory-exists? (get-resource "seqc/sample-project"))
    (let [tar-file
          (str (fs/file (get-resource "seqc") (fs/base-name data-url)))]
      (println  (format "Downloading %s to %s." data-url tar-file))
      (download-file data-url tar-file)
      (sh "tar" "-xvf" tar-file "-C" (dirname tar-file)))))

(defn setup-test-data []
  (install-test-data)
  (fix-project-config)
  (setup-config (default-bcbio-project)))

(defn teardown-test-data []
  (let [sample-project-dir (get-resource "seqc/sample-project")]
    (alter-config! {})
    (when (directory-exists? sample-project-dir)
      (fs/delete-dir sample-project-dir))))

(defn test-setup [f]
  (setup-test-data)
  (f)
  (teardown-test-data))
