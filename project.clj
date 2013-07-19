(defproject bcbio.rnaseq "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [de.ubercode.clostache/clostache "1.3.1"]
                 [incanter/incanter-io "1.5.1"]
                 [me.raynes/fs "1.4.4"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]]}})
