# Development

## Codevelopment with the EISOP Checker Framework

This project depends on the [EISOP Checker Framework][EISOP].

To codevelop changes with the EISOP Checker Framework, clone it into the
sibling directory `../checker-framework` and pass
`--include-build path/to/checker-framework` to Gradle when building
this project.

This project's build expects [JSpecify's annotated JDK][jspecify-jdk] in
`../jdk`, but the Checker Framework's build expects its own annotated JDK
there. Either pass `-PskipAnnotatedJdk` so that the Checker Framework is
built without one, as this project's CI does, or clone JSpecify's JDK
elsewhere and pass `-PjspecifyJdkHome=path/to/jdk`, as the Checker
Framework's `checker/bin-devel/test-jspecify-reference-checker.sh` does.

## Codevelopment with JSpecify

This project depends on two artifacts built from the main [JSpecify repo]
[jspecify-jspecify]:

1. the annotations: `org.jspecify:jspecify`
2. the conformance test suite: `org.jspecify.conformance:conformance-tests`

For each of those dependencies, developers can depend on either a published
version (fixed or snapshot) or a local build.

In order to depend on a specific commit or uncommitted state of those artifacts,
clone the repo (or your fork) somewhere, and pass
`--include-build path/to/jspecify` to Gradle when building. The local clone will
be used for both the annotations and the conformance test suite.

By default the reference checker depends on version `1.0.0` of the annotations,
and version `0.0.0-SNAPSHOT` of the conformance test suite.

In order to depend on a different published version of either artifact, set
Gradle properties on the command line.

* `-Porg.jspecify:jspecify=0.2.0` would change the version of the annotations.
* `-Porg.jspecify.conformance:conformance-tests=0.3.0` would change the version
  of the conformance test suite.

[EISOP]: https://github.com/eisop/checker-framework
[jspecify-jdk]: https://github.com/jspecify/jdk
[jspecify-jspecify]: https://github.com/jspecify/jspecify
