(ns changelog.main
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh])
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

(def ^:dynamic *exit-process?*
  "Bind to false to suppress process termination." true)

(def ^:dynamic *info* (not (System/getenv "CHANGELOG_SILENT")))

(defn info
  "Print if *info* (from LEIN_SILENT environment variable) is truthy."
  [& args]
  (when *info* (apply println args)))

(defn warn
  "Print to stderr if *info* is truthy."
  [& args]
  (when *info*
    (binding [*out* *err*]
      (apply println args))))

(defn exit
  "Exit the process. Rebind *exit-process?* in order to suppress actual process
  exits for tools which may want to continue operating. Never call
  System/exit directly in Leiningen's own process."
  ([exit-code & msg]
   (if *exit-process?*
     (do (shutdown-agents)
         (System/exit exit-code))
     (throw (ex-info (if (seq msg)
                       (apply print-str msg)
                       "Suppressed exit")
                     {:exit-code exit-code :suppress-msg (empty? msg)}))))
  ([] (exit 0)))

(def unreleased-line-pattern "## [Unreleased]")
(def unreleased-link-line-pattern #"\[Unreleased\]: .*/compare/([^/]+)...HEAD")


(defn tokenize-line [line]
  (let [line (str/trim line)]
    (if (= unreleased-line-pattern line)
      [::unreleased]
      (if-let [[[_ version]] (re-seq unreleased-link-line-pattern line)]
        [::unreleased-link version]
        nil))))


(defn process-line [acc line]
  (let [{:keys [found-unreleased-title]} acc
        [token value] (tokenize-line line)
        append-to-key (if found-unreleased-title
                        :after-unreleased
                        :before-unreleased)]
    (case token
      ::unreleased (assoc acc :found-unreleased-title line)
      ::unreleased-link (-> acc
                            (assoc :last-released-version value)
                            (assoc :unreleased-link line))
      (update acc append-to-key (fnil conj []) line))))


(defn parse-changelog [changelog-str]
  (let [lines (str/split-lines changelog-str)]
    (reduce process-line
            {}
            lines)))


(defn render-changelog [changelog-data next-version today-date]
  (let [{:keys [before-unreleased found-unreleased-title after-unreleased unreleased-link last-released-version]} changelog-data
        next-version-title  (format "## [%s] — %s" next-version today-date)
        next-version-link   (-> unreleased-link
                                (str/replace "[Unreleased]" (str "[" next-version "]"))
                                (str/replace "...HEAD" (str "..." next-version)))
        new-unreleased-link (-> unreleased-link
                                (str/replace last-released-version next-version))]
    (str/join "\n"
              (concat before-unreleased
                      [found-unreleased-title
                       ""
                       next-version-title]
                      after-unreleased
                      [next-version-link
                       new-unreleased-link
                       ""]))))


(defn release-impl [next-version today-date changelog-str]
  (let [changelog-data (parse-changelog changelog-str)]
    (if-not (:found-unreleased-title changelog-data)
      (exit 1 (format "'%s' line not found, cannot figure out which section contains next version's feature descriptions." unreleased-line-pattern))
      (if-not (:last-released-version changelog-data)
        (exit 1 (format "'%s' line not found, cannot figure out which version was released previously and how a comparison link between it and the latest commit would look like." unreleased-link-line-pattern))
        (render-changelog changelog-data next-version today-date)))))


(def changelog-filename "CHANGELOG.md")


(defn prompt-overwrite []
  (if-not (.exists (io/file changelog-filename))
    true
    (do
      (print (str changelog-filename " file found. Do you want to overwrite it? [y/N] "))
      (flush)
      (let [response (str/trim (read-line))]
        (#{"y" "Y"} response)))))


(def git-repo-regex #"github.com(?:/|:)([^/]+/[^/]+)")


(defn extract-owner+repo [git-remote-url]
  (let [trimmed   (str/trim git-remote-url)
        truncated (str/replace trimmed #".git$" "")
        [[_ owner+repo]] (re-seq git-repo-regex truncated)]
    (when-not owner+repo
      (info "Cannot figure out owner and repo from remote URL:" trimmed))
    owner+repo))


(defn get-owner+repo []
  (let [{:keys [exit out]} (sh/sh "git" "remote" "get-url" "origin")]
    (if (zero? exit)
      (extract-owner+repo out)
      (info "Git repository not found in the current directory."))))


(defn generate-changelog-str [template-str owner+repo last-version last-version-date]
  (when-not owner+repo
    (info "Using \"OWNER/REPO\" in the generated changelog file. You should replace it later."))
  (-> template-str
      (str/replace "{{owner+repo}}" (or owner+repo "OWNER/REPO"))
      (str/replace "{{last-version}}" last-version)
      (str/replace "{{last-version-date}}" last-version-date)))


(defn get-today-date []
  (.format (SimpleDateFormat. "yyyy-MM-dd") (Date.)))


(defn get-latest-tag []
  (let [{:keys [exit out err]} (sh/sh "git" "describe" "--tags" "--abbrev=0")]
    (if (zero? exit)
      (str/trim out)
      (info "No tags found in current repo:" err))))


(defn get-tag-date [tag]
  (let [{:keys [exit out err]} (sh/sh "git" "--no-pager" "log" "-1" "--format=%ad" "--date=short" tag)]
    (if (zero? exit)
      (str/trim out)
      (info "Cannot get tag date:" err))))


(defn init [{:keys [version]}]
  (if-not (prompt-overwrite)
    (exit 1)
    (let [template-str    (slurp (io/resource "templates/CHANGELOG.md"))
          latest-tag      (get-latest-tag)
          latest-tag-date (when latest-tag
                            (get-tag-date latest-tag))
          today-date      (get-today-date)
          owner+repo      (get-owner+repo)
          changelog-str   (generate-changelog-str template-str owner+repo (or latest-tag version) (or latest-tag-date today-date))]
      (info "Wrote" changelog-filename)
      (spit changelog-filename changelog-str))))


(defn release [{:keys [version]}]
  (if-not (.exists (io/file changelog-filename))
    (warn changelog-filename "not found, use `lein changelog init` to create one.")
    (let [changelog-str (slurp changelog-filename)
          today-date    (get-today-date)]
      (->> (release-impl version today-date changelog-str)
           (spit changelog-filename)))))

(defn run
  [{:keys [task version] :or {task "init" version "0.0.0"}}]
  (case task
    "init" (init {:version version})
    "release" (release {:version version})
    nil :not-implemented-yet
    (do (warn "Unknown task.")
        (exit 1 "Unknown task.")))
  (exit 0))

(defn -main
  [task version]
  (run {:task task :version version}))


(comment
  (release-impl "0.2.0"
                "2018-18-18"
                (slurp "test/test-changelog.before.md")
                ))
