(defproject bcbio.rnaseq "1.2.0"
  :description "Quality control and differential expression of bcbio RNA-seq runs"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [version-clj "0.1.2"]
                 [incanter "1.5.5"]
                 [clj-stacktrace "0.2.5"]
                 [incanter/incanter-io "1.5.5"]
                 [me.raynes/fs "1.4.4"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/data.json "0.2.2"]
                 [clj-http "0.7.5"]
                 [net.mikera/vectorz-clj "0.22.0"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [bcbio.run "0.0.1"]
                 [clj-http "0.7.8"]]
  :resource-paths ["resources"]
  :jvm-opts ["-Xmx2g"]
  :aot [bcbio.rnaseq.core]
  :main bcbio.rnaseq.core
  :keep-non-project-classes true
  :test-selectors {:default (complement :slow)
                   :slow :slow
                   :all (constantly true)})
