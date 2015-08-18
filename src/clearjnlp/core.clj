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

(ns clearjnlp.core
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
    (edu.emory.clir.clearnlp.component.utils NLPUtils CFlag)
    (java.io ByteArrayInputStream ObjectInputStream)
    (java.util List)
    (edu.emory.clir.clearnlp.component.mode.dep.state DEPStateBranch) )
  (:require
    [clojure.string :as str]
    [clojure.tools.logging]
    [clj-logging-config.log4j :as cljlog]
    [flatland.ordered.map :as om] ))


(set! *warn-on-reflection* true)
(cljlog/set-logger! :level :warn)

(defmacro proxy-super-cls [cls meth & args]
  "Macro provides a type hint on this so we can call super class methods without reflection.
  Unfortunately this only works for public methods, not protected."
  (let [thissym (with-meta (gensym) {:tag cls})]
    `(let [~thissym ~'this]
       (proxy-call-with-super (fn [] (. ~thissym ~meth ~@args)) ~thissym ~(name meth)))))

(def ^:private init-clearnlp
  (memoize (fn []
             (GlobalLexica/initDistributionalSemanticsWords
              ["brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz"])
             (GlobalLexica/initNamedEntityDictionary
              "general-en-ner-gazetteer.xz"))))

(def ^:private get-scoring-dep-parser
  "proxy a dep parser to generate scores"
  (memoize (fn [model]
             (let [score (atom 0.0)
                   ^DEPConfiguration depconf (DEPConfiguration. "root")
                   ^ObjectInputStream model-stream (NLPUtils/getObjectInputStream model)]
               (proxy [AbstractDEPParser clojure.lang.IDeref] [depconf model-stream]
                 (process
                   [tree]
                   (let [branch (DEPStateBranch. tree CFlag/DECODE depconf)
                         instances (proxy-super process branch)]
                     (if (.startBranching branch)
                       (do
                         (while
                             (.nextBranch branch)
                           (.saveBest branch (proxy-super-cls AbstractDEPParser process branch)))
                         (def tmp ^List (.setBest branch))
                         (if tmp (.addAll ^List instances tmp))))
                     (proxy-super-cls AbstractDEPParser processHeadless branch)
                     (if (.-best_tree branch)
                       (reset! score (.-d (.-best_tree branch)))
                       (reset! score (.getScore branch)))))
                 (deref [] score))))))

(def ^:private ^AbstractTokenizer get-tokenizer
  "Memoized function that returns a tokenizer."
  (memoize
    (fn [language]
      (NLPUtils/getTokenizer language))))

(defn tokenize
  "Tokenize a string, with an optional language."
  ([language ^String s]
   (.tokenize (get-tokenizer language)
              (ByteArrayInputStream. (.getBytes s "UTF-8"))))
  ([s]
   (tokenize TLanguage/ENGLISH s)))

(defn segment-sentences
  "Tokenize a string, with an optional language."
  ([language ^String s]
   (.segmentize (get-tokenizer language)
                (ByteArrayInputStream. (.getBytes s "UTF-8"))))
  ([s]
   (segment-sentences TLanguage/ENGLISH s)))

(def ^:private ^AbstractPOSTagger get-pos-tagger
  "Memoized function that returns a POS tagger."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getPOSTagger language model))))

(defn pos-tag
  "Tag a sequence of tokens."
  ([language model tree]
   (.process (get-pos-tagger language model) tree)
    tree)
  ([tree]
   (pos-tag TLanguage/ENGLISH "general-en-pos.xz" tree)))

(def ^:private ^AbstractMPAnalyzer get-morph-analyzer
  "Memoized function that return a morphological analyzer."
  (memoize
    (fn [^TLanguage language]
      (NLPUtils/getMPAnalyzer language))))

(defn morph-analyze
  "Tag tree with lemmas."
  ([language tree]
   (.process (get-morph-analyzer language) tree)
    tree)
  ([tree]
   (morph-analyze TLanguage/ENGLISH tree)))


(defn score-dep-parse
  "Parse the tree"
  ([language model tree]
   (let [parser (get-scoring-dep-parser model)]
     (.process ^AbstractDEPParser parser tree)
     {:tree tree :score @@parser}))
  ([tree]
   (score-dep-parse TLanguage/ENGLISH "general-en-dep.xz" tree)))

(def ^:private ^AbstractDEPParser get-dep-parser
  "Memoized function that returns a dependency parser."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (let [depconf (DEPConfiguration. "root")]
        (NLPUtils/getDEPParser language
                               model
                               depconf)))))

(defn dep-parse
  "Parse the tree"
  ([language model tree]
   (.process (get-dep-parser language model) tree)
    tree)
  ([tree]
   (dep-parse TLanguage/ENGLISH "general-en-dep.xz" tree)))

(def ^:private ^AbstractSRLabeler get-sr-labeller
  "Memoized function that returns a semantic relationship labeller."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getSRLabeler language model (SRLConfiguration. 4 3)))))

(defn sr-label
  "Annotate the tree with semantic relationship labels"
  ([language model tree]
   (.process (get-sr-labeller language model) tree)
    tree)
  ([tree]
   (sr-label TLanguage/ENGLISH "general-en-srl.xz" tree)))

(def ^:private ^AbstractNERecognizer get-ner-labeller
  "Memoized function that returns a named entity recognition labeller."
  (memoize
    (fn [^TLanguage language
         ^String model]
      (NLPUtils/getNERecognizer language model))))

(defn ner-label
  "Annotate the tree with named entities"
  ([language model tree]
   (.process (get-ner-labeller language model) tree)
    tree)
  ([tree]
   (ner-label TLanguage/ENGLISH "general-en-ner.xz" tree)))

(defn deptree-to-map
  "Generate a map from a DEPTree"
  [^DEPTree tree]
  (let [fields [:id :word :lemma :pos :feats :head :s-label :s-heads :x-heads :entity]
        nodes (str/split (.toString tree) #"\n")]
    (mapv #(apply assoc (om/ordered-map)
                  (interleave fields (str/split % #"\t"))) nodes)))

(defn- tokenize-newlines
  "Change mid-word newlines inside a string into a new token"
  [s]
  (str/replace s #"(?<=\w)\n(?=\w)" " NEWLINE_TOKEN "))

(defn- conditionally-split
  "Take a sentence as a seq of tokens and conditionally split into two sentences
  at newlines"
  [sentence]
  (let [score-sentence (comp score-dep-parse morph-analyze pos-tag #(DEPTree. ^List %))]
        (if (some #{"NEWLINE_TOKEN"} sentence)
          (let [partitioned-sentences (partition-by #(= "NEWLINE_TOKEN" %) sentence)
                split-sentences (filter
                                 (complement #(some #{"NEWLINE_TOKEN"} %))
                                 partitioned-sentences)
                split-trees (map score-sentence split-sentences)
                split-trees-score (- (:score (apply min-key :score split-trees)) 0.00)
                whole-tree (score-sentence (filter #(not= "NEWLINE_TOKEN" %) sentence))
                whole-score (:score whole-tree)]
            (if (>= split-trees-score whole-score)
              (map :tree split-trees)
              (list (:tree whole-tree))))
          (list (:tree (score-sentence sentence))))))

(defn- post-parse
  "Take a tree that been dependency parsed and run SRL and NER."
  [tree]
  ((comp ner-label sr-label) tree))

(defn pipeline
  "Run the nlp pipeline on document."
  [document]
  (let [pipe (comp (partial map (comp ner-label sr-label dep-parse morph-analyze pos-tag))
                   #(DEPTree. ^List %) segment-sentences)]
    (init-clearnlp)
    pipe document))

(defn pipeline-scored
  "Run the pipeline, but use the scoring parser to help sentence segmentation"
  [document]
  (let [pipe (comp (partial map post-parse)
                   (partial mapcat conditionally-split) segment-sentences)]
    (init-clearnlp)
    (pipe document)))
