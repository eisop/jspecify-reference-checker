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

// @below-java21-jdk-skip-test

// A type pattern nested in a record pattern that is unconditional for the record component's type
// matches null, so its variable has the nullness of the record component, not of the operand. A
// nested type pattern that is not unconditional does not match null, so its variable is non-null.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class RecordPatternBinding {
  record NullableComponent(@Nullable String f) {}

  record NonNullComponent(String g) {}

  record ObjectComponent(@Nullable Object h) {}

  record Box<T extends @Nullable Object>(T t) {}

  int nullableComponent(Object o) {
    if (o instanceof NullableComponent(String s)) {
      // :: error: (dereference)
      return s.length();
    }
    return 0;
  }

  int nullableComponentVar(Object o) {
    if (o instanceof NullableComponent(var s)) {
      // :: error: (dereference)
      return s.length();
    }
    return 0;
  }

  int nullableComponentSwitch(Object o) {
    return switch (o) {
      // :: error: (dereference)
      case NullableComponent(String s) -> s.length();
      default -> 0;
    };
  }

  int nonNullComponentNullableOperand(@Nullable Object o) {
    if (o instanceof NonNullComponent(String s)) {
      return s.length();
    }
    return 0;
  }

  int notUnconditional(Object o) {
    if (o instanceof ObjectComponent(String s)) {
      return s.length();
    }
    return 0;
  }

  <T extends @Nullable Object> T typeVariableComponent(Box<T> b, T other) {
    if (b instanceof Box<T>(var x)) {
      return x;
    }
    return other;
  }
}
