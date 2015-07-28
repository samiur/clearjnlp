; Copyright 2015 Cirruspath, Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns clearnlp-clj.core
  (:import
    (edu.emory.clir.clearnlp.component.utils NLPUtils GlobalLexica)
    (edu.emory.clir.clearnlp.util.lang TLanguage)
    (edu.emory.clir.clearnlp.tokenization AbstractTokenizer)
    (edu.emory.clir.clearnlp.component.mode.dep DEPConfiguration
                                                AbstractDEPParser)
    (edu.emory.clir.clearnlp.component.mode.pos AbstractPOSTagger)
    (edu.emory.clir.clearnlp.dependency DEPTree)
    (edu.emory.clir.clearnlp.component.mode.morph AbstractMPAnalyzer)
    (edu.emory.clir.clearnlp.component.mode.srl SRLConfiguration AbstractSRLabeler)
    (edu.emory.clir.clearnlp.component.mode.ner AbstractNERecognizer)
    (java.io StringBufferInputStream)
    (java.util List))
  (:require
    [clojure.string :as str]
    [clojure.tools.logging]
    [clj-logging-config.log4j :as cljlog]
    [flatland.ordered.map :as om]))

(set! *warn-on-reflection* true)
(cljlog/set-logger! :level :warn)

(def init-clearnlp
  (memoize (fn []
             (GlobalLexica/initDistributionalSemanticsWords
               ["brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz"])
             (GlobalLexica/initNamedEntityDictionary
               "general-en-ner-gazetteer.xz"))))

(def ^AbstractTokenizer get-tokenizer
  "Memoized function that returns a tokenizer."
  (memoize
    (fn [language]
      (NLPUtils/getTokenizer language))))

(defn tokenize
  "Tokenize a string, with an optional language."
  ([language s]
   (.tokenize (get-tokenizer language)
              (StringBufferInputStream. s)))
  ([s]
   (tokenize TLanguage/ENGLISH s)))

(defn segment-sentences
  "Tokenize a string, with an optional language."
  ([language s]
   (.segmentize (get-tokenizer language)
                (StringBufferInputStream. s)))
  ([s]
   (segment-sentences TLanguage/ENGLISH s)))

(def ^AbstractPOSTagger get-pos-tagger
  "Memoized function that returns a POS tagger."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getPOSTagger language model))))

(defn pos-tag
  "Tag a sequence of tokens."
  ([language model tree]
   (.process (get-pos-tagger language model) tree))
  ([tree]
   (pos-tag TLanguage/ENGLISH "general-en-pos.xz" tree)))

(def ^AbstractMPAnalyzer get-morph-analyzer
  "Memoized function that return a morphological analyzer."
  (memoize
    (fn [^TLanguage language]
      (NLPUtils/getMPAnalyzer language))))

(defn morph-analyze
  "Tag tree with lemmas."
  ([language tree]
   (.process (get-morph-analyzer language) tree))
  ([tree]
   (morph-analyze TLanguage/ENGLISH tree)))

(def ^AbstractDEPParser get-dep-parser
  "Memoized function that returns a dependency parser."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getDEPParser language
                             model
                             (DEPConfiguration. "root")))))

(defn dep-parse
  "Parse the tree"
  ([language model tree]
   (.process (get-dep-parser language model) tree))
  ([tree]
   (dep-parse TLanguage/ENGLISH "general-en-dep.xz" tree)))

(def ^AbstractSRLabeler get-sr-labeller
  "Memoized function that returns a semantic relationship labeller."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getSRLabeler language model (SRLConfiguration. 4 3)))))

(defn sr-label
  "Annotate the tree with semantic relationship labels"
  ([language model tree]
   (.process (get-sr-labeller language model) tree))
  ([tree]
   (sr-label TLanguage/ENGLISH "general-en-srl.xz" tree)))

(def ^AbstractNERecognizer get-ner-labeller
  "Memoized function that returns a named entity recognition labeller."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getNERecognizer language model))))

(defn ner-label
  "Annotate the tree with named entities"
  ([language model tree]
   (.process (get-ner-labeller language model) tree))
  ([tree]
   (ner-label TLanguage/ENGLISH "general-en-ner.xz" tree)))

(defn deptree-to-map
  "Generate a map from a DEPTree"
  [^DEPTree tree]
  (let [fields [:id :word :lemma :pos :feats :head :s-label :s-heads :x-heads :entity]
        nodes (str/split (.toString tree) #"\n")]
    (mapv #(apply assoc (om/ordered-map)
                  (interleave fields (str/split % #"\t"))) nodes)))

(defn pipeline
  "Run the nlp pipeline on document."
  [document]
  (let [annotators [pos-tag morph-analyze dep-parse sr-label ner-label]
        sentences (segment-sentences document)
        dep-trees (map #(DEPTree. ^List %) sentences)
        annotate (fn [tree] (doseq [ann annotators] (ann tree)))]
    (init-clearnlp)
    (doseq [tree dep-trees] (annotate tree))
    (mapv deptree-to-map dep-trees)))

