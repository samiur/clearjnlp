(ns clearnlp-clj.core-test
  (:require [clojure.test :refer :all]
            [clearnlp-clj.core :refer :all])
  (:import (edu.emory.clir.clearnlp.util.lang TLanguage)))

(deftest pipeline-test
  (testing "pipeline"
    (is (=
          (clearnlp-clj.core/pipeline (TLanguage/ENGLISH))
          0))))
