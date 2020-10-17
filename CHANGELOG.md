# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).


## [Unreleased]
### Added
- clojure -X usage explanation

## [0.1.0] — 2020-10-17
### Changed
- Fork from https://github.com/dryewo/lein-changelog/commit/2a50a8a267bac7710c5689447e597032aefa498a
- Remove dependency on lein and make it a standalone tool
- Rename leiningen.changelog to changelog.main
- Update .gitignore
- Update README
### Added
- fresh _CHANGELOG.md_ created.
- Copy the functions from lein needed by the current code
- Use kaocha for unit testing
- Use kaocha-cloverage for code coverage
- Add some scripts for making new releases and a Makefile
- Add deps.edn
- Add pom.xml
- Add github workflow for testing
### Fixed
- Fix issue with main not exiting
### Removed
- project.clj
- travis support

## 0.0.0 — 2020-10-09
Released without _CHANGELOG.md_.


[0.1.0]: https://github.com/ageneau/keepachangelog/compare/0.0.0...0.1.0
[Unreleased]: https://github.com/ageneau/keepachangelog/compare/0.1.0...HEAD
