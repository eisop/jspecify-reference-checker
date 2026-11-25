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

// @SuppressWarnings("nullness") on a class, method, or variable skips checking it entirely,
// including nested classes and lambdas; other code is still checked.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class SuppressWarningsNullnessSkipsDefinitions {
  Object checked(@Nullable Object o) {
    // :: error: (return.type.incompatible)
    return o;
  }

  @SuppressWarnings("nullness")
  Object suppressedMethod(@Nullable Object o) {
    return o;
  }

  @Nullable Object nullable;

  // :: error: (assignment.type.incompatible)
  Object checkedField = nullable;

  @SuppressWarnings("nullness")
  Object suppressedField = nullable;

  @SuppressWarnings("nullness")
  Runnable suppressedLambda =
      () -> {
        Object o = nullable;
      };

  void locals() {
    // :: error: (dereference)
    String checkedLocal = nullable.toString();
    @SuppressWarnings("nullness")
    String suppressedLocal = nullable.toString();
  }

  @SuppressWarnings("nullness")
  static class SuppressedClass {
    Object m(@Nullable Object o) {
      return o;
    }

    class Nested {
      Object n(@Nullable Object o) {
        return o;
      }
    }
  }
}
