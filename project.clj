(defproject clearjnlp "0.1.0-SNAPSHOT"
  :description "Clojure bindings for CLIR ClearNLP"
  :url "none"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [edu.emory.clir/clearnlp "3.2.1.CC-SNAPSHOT"]
                 [edu.emory.clir/clearnlp-dictionary "3.2"]
                 [edu.emory.clir/clearnlp-global-lexica "3.1"]
                 [edu.emory.clir/clearnlp-general-en-pos "3.2"]
                 [edu.emory.clir/clearnlp-general-en-dep "3.2"]
                 [edu.emory.clir/clearnlp-general-en-srl "3.0"]
                 [edu.emory.clir/clearnlp-general-en-ner "3.1"]
                 [edu.emory.clir/clearnlp-general-en-ner-gazetteer "3.0"]
                 [org.flatland/ordered "1.5.3"]
                 [clj-logging-config "1.9.12"]]
  :jvm-opts ["-Xmx8g"])
