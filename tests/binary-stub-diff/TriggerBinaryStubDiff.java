// The binary-stub differential check runs once, while the checker initializes: for every class in
// the annotated JDK's binary stub file, it compares the annotations the binary produces against the
// ones the text parser produces from the JDK sources beside it, and reports any disagreement as an
// error. What is compiled here is therefore irrelevant -- this file exists only to make the checker
// run. See the binaryStubDiffTest task in build.gradle.
class TriggerBinaryStubDiff {}
