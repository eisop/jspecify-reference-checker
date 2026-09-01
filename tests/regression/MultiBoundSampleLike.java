// Copyright 2025 The JSpecify Authors
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

// Tests mimicking jspecify's MultiBoundTypeVariableToObject.java and
// MultiBoundTypeVariableToOther.java samples. These cover cases with explicit
// java.lang.Object as the first intersection bound alongside @Nullable bounds.
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class MultiBoundSampleLike {
  // x0: T extends Object & Lib -> both non-null -> no error
  <T extends Object & Lib> Object x0(T x) {
    return x;
  }

  // x2: T extends Object & @Nullable Lib -> Object is non-null -> no error
  <T extends Object & @Nullable Lib> Object x2(T x) {
    return x;
  }

  // x6: T extends @Nullable Object & Lib -> Lib is non-null -> no error
  <T extends @Nullable Object & Lib> Object x6(T x) {
    return x;
  }

  // x8: T extends @Nullable Object & @Nullable Lib -> both nullable -> error
  <T extends @Nullable Object & @Nullable Lib> Object x8(T x) {
    // :: error: jspecify_nullness_mismatch
    return x;
  }

  // Same tests but returning as Lib (non-Object supertype)
  <T extends Object & @Nullable Lib> Lib x2ToOther(T x) {
    return x;
  }

  <T extends @Nullable Object & Lib> Lib x6ToOther(T x) {
    return x;
  }

  <T extends @Nullable Object & @Nullable Lib> Lib x8ToOther(T x) {
    // :: error: jspecify_nullness_mismatch
    return x;
  }

  interface Lib {}
}
