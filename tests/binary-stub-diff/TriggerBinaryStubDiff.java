// Copyright 2026 The JSpecify Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// The binary-stub differential check runs once, while the checker initializes: for every class in
// the annotated JDK's binary stub file, it compares the annotations the binary produces against the
// ones the text parser produces from the JDK sources beside it, and reports any disagreement as an
// error. What is compiled here is therefore irrelevant -- this file exists only to make the checker
// run. See the binaryStubDiffTest task in build.gradle.
class TriggerBinaryStubDiff {}
