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

(ns clearnlp-clj.core-test
  (:require [clojure.test :refer :all]
            [clearnlp-clj.core :refer :all]))

(deftest pipeline-test
  (testing "pipeline"
    (is (=
          (clearnlp-clj.core/pipeline "I hate Bozo because he is a clown.")
          [[{:id      "1",
             :word    "I",
             :lemma   "I",
             :pos     "PRP",
             :feats   "_",
             :head    "2",
             :s-label "nsubj",
             :s-heads "2:A0",
             :x-heads "_",
             :entity  "O"}
            {:id      "2",
             :word    "hate",
             :lemma   "hate",
             :pos     "VBP",
             :feats   "pb=hate",
             :head    "0",
             :s-label "root",
             :s-heads "_",
             :x-heads "_",
             :entity  "O"}
            {:id      "3",
             :word    "Bozo",
             :lemma   "bozo",
             :pos     "NNP",
             :feats   "_",
             :head    "2",
             :s-label "dobj",
             :s-heads "2:A1",
             :x-heads "_",
             :entity  "U-PERSON"}
            {:id      "4",
             :word    "because",
             :lemma   "because",
             :pos     "IN",
             :feats   "_",
             :head    "6",
             :s-label "mark",
             :s-heads "_",
             :x-heads "_",
             :entity  "O"}
            {:id      "5",
             :word    "he",
             :lemma   "he",
             :pos     "PRP",
             :feats   "_",
             :head    "6",
             :s-label "nsubj",
             :s-heads "6:A1",
             :x-heads "_",
             :entity  "O"}
            {:id      "6",
             :word    "is",
             :lemma   "be",
             :pos     "VBZ",
             :feats   "pb=be",
             :head    "2",
             :s-label "advcl",
             :s-heads "2:CAU",
             :x-heads "_",
             :entity  "O"}
            {:id      "7",
             :word    "a",
             :lemma   "a",
             :pos     "DT",
             :feats   "_",
             :head    "8",
             :s-label "det",
             :s-heads "_",
             :x-heads "_",
             :entity  "O"}
            {:id      "8",
             :word    "clown",
             :lemma   "clown",
             :pos     "NN",
             :feats   "_",
             :head    "6",
             :s-label "attr",
             :s-heads "6:PRD",
             :x-heads "_",
             :entity  "O"}
            {:id      "9",
             :word    ".",
             :lemma   ".",
             :pos     ".",
             :feats   "_",
             :head    "2",
             :s-label "punct",
             :s-heads "_",
             :x-heads "_",
             :entity  "O"}]]))))
