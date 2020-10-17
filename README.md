# keepachangelog
[![Clojars Project](https://img.shields.io/clojars/v/ageneau/keepachangelog.svg)](https://clojars.org/ageneau/keepachangelog)

A Clojure tool to automate changelog tasks.

In support for [Keep a Changelog] initiative, relies on the changelog format proposed there.

Intended to be used as part of an automated release procedure.


## Usage

First, add an alias to your _deps.edn_:

```clj
{
 :aliases {
           :changelog {
                       :extra-deps {ageneau/keepachangelog {:mvn/version "0.1.0"}}
                       :main-opts   ["-m" "changelog.main"]}
           }
}
```

### If you don't have _CHANGELOG.md_

Run this command to generate a dummy _CHANGELOG.md_ file from template (replace the version by your own):

    $ clj -M:changelog init "0.1.0"

Open the freshly generated _CHANGELOG.md_ file and check its contents.
You might want to correct the intro part and add some details to the the last released version section as well
(it was generated from the latest git tag).

As you work on your project, add [notable changes](https://keepachangelog.com/en/1.0.0/#how) to `## [Unreleased]`
section with every commit you make.

### If you already have _CHANGELOG.md_

If you didn't use `clj -M:changelog init` to create it, make sure that it corresponds to the [format](#changelog-format).

When you are ready to release the next version, just run:

    $ clj -M:changelog release "0.1.1"

Your _CHANGELOG.md_ will be updated.


## Explanation

When you run

    $ clj -M:changelog release "${NEW_VERSION}"

This tool does the following:

1. Reads contents of the _CHANGELOG.md_ file.
2. Replaces `## [Unreleased]` line with `## [X.Y.Z] - 2018-18-18`,  
   where `X.Y.Z` is the version from _project.clj_ and `2018-18-18` is today's date.  
3. Adds a new empty section with `## [Unreleased]` title on top of the file.
4. Inserts links to GitHub diff UI to the end of the file:
   * `[X.Y.Z]` to show differences between this released version and previously released one.
   * `[Unreleased]` to show new changes since the latest released version.

Example:

Given this _CHANGELOG.md_:

```
blah-blah

## [Unreleased]
new-blah

## [0.1.1] - 2018-06-01
old-blah

## 0.1.0 - 2018-01-01
initial-blah

[0.1.1]: https://github.com/your-name/your-repo/compare/0.1.0...0.1.1
[Unreleased]: https://github.com/your-name/your-repo/compare/0.1.1...HEAD
```

After running `clj -M:changelog release "0.2.0"`,
it updates _CHANGELOG.md_ like this (new version being released is `0.2.0`):

```
blah-blah

## [Unreleased]

## [0.2.0] - 2018-18-18
new-blah

## [0.1.1] - 2018-06-01
old-blah

## 0.1.0 - 2018-01-01
initial-blah

[0.1.1]: https://github.com/your-name/your-repo/compare/0.1.0...0.1.1
[0.2.0]: https://github.com/your-name/your-repo/compare/0.1.1...0.2.0
[Unreleased]: https://github.com/your-name/your-repo/compare/0.2.0...HEAD
```

## `clojure -X` Usage

The Clojure CLI added an `-X` option (in 1.10.1.697) to execute a specific function and pass a hash map of arguments. See [Executing a function that takes a map](https://clojure.org/reference/deps_and_cli#_executing_a_function) in the Deps and CLI reference for details.

This is supported this via `changelog.main/run` which accepts a hash map that mirrors the available command-line arguments:

* `:task` -- can be "init" or "release".
* `:version` -- when task is "init" specifies the initial release and when task is "release" specifies the new version to be released.

The following commands are equivalent:

```bash
clojure -M:changelog release '0.1.0'

clojure -X:changelog changelog.main/run :task '"release"' :version '"0.1.0"'
```

### Changelog format

This tool relies on the format described on [Keep a Changelog], but the only required parts here are:

1. The file has to be named "CHANGELOG.md" and located in the root of the repository.
2. `## [Unreleased]` line has be present in _CHANGELOG.md_ exactly.
3. There has to be a line that looks like `[Unreleased]: https://github.com/your-name/your-repo/compare/A.B.C...HEAD`  
   It will be copied and updated to create a diff link to the version currently being released (`X.Y.Z`) as well as
   to the new `[Unreleased]` diff.

If any of these lines are missing, the plugin will fail and exit with a non-zero code. 


## License

Copyright © 2018 Dmitrii Balakhonskii

Copyright © 2020 Sylvain Ageneau

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[Keep a Changelog]: https://keepachangelog.com
