(ns changelog.main-test
  (:require [clojure.test :refer (deftest are testing is)]
            [changelog.main :as ch])
  (:import (clojure.lang ExceptionInfo)))


(deftest about-tokenize-line
  (are [?line ?res]
    (= ?res (ch/tokenize-line ?line))
    "nothing" nil
    "## [Unreleased]" [::ch/unreleased]
    "  ## [Unreleased]  " [::ch/unreleased]
    "[Unreleased]: foo blah blah /compare/1.2.3...HEAD" [::ch/unreleased-link "1.2.3"]
    "[Unreleased]: foo blah blah /compare/mary had a little lamb...HEAD" [::ch/unreleased-link "mary had a little lamb"]))


(deftest about-extract-owner+repo
  (are [?in ?out]
    (= ?out (ch/extract-owner+repo ?in))
    "git@github.com/foo/bar" "foo/bar"
    "git@github.com/foo/bar.git" "foo/bar"
    "git@github.com/foobar.git" nil))


(deftest works
  (testing "Happy case"
    (is (= (slurp "test/test-changelog.after.md")
           (ch/release-impl "0.2.0" "2018-18-18" (slurp "test/test-changelog.before.md")))))

  (testing "Errors"
    (are [?in ?error]
      (re-seq ?error
              (binding [ch/*exit-process?* false]
                (try
                  (ch/release-impl "0.2.0" "1234-56-78" ?in)
                  ::not-thrown
                  (catch ExceptionInfo e
                    (str e)))))
      "" #"'## \[Unreleased\]' line not found"
      "## [Unreleased]" #"HEAD' line not found")))
