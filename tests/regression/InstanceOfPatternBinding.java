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

// The variable of an instanceof binding pattern is non-null where the test holds, even if the
// operand is nullable, and the pattern is not reported as unsafe.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class InstanceOfPatternBinding {
  int conditional(@Nullable Object o) {
    return o instanceof String s ? s.length() : 0;
  }

  int ifStatement(@Nullable Object o) {
    if (o instanceof String s) {
      return s.length();
    }
    return 0;
  }

  int negated(@Nullable Object o) {
    if (!(o instanceof String s)) {
      return 0;
    }
    return s.length();
  }

  int elseBranch(@Nullable Object o) {
    if (o instanceof String s) {
      return s.length();
    } else {
      // :: error: (dereference)
      return o.hashCode();
    }
  }
}
