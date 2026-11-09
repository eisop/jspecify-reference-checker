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

// Unboxing a null reference throws NPE (JLS 5.1.8), so an operand that is unboxed must not be
// nullable; neither may the selector of a switch without `case null`. A string concatenation and a
// comparison of two references do not unbox.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class NullableUnboxing {
  int binary(@Nullable Integer i) {
    // :: error: dereference
    return i + 1;
  }

  boolean comparison(@Nullable Integer i) {
    // :: error: dereference
    return i < 1;
  }

  boolean equalToPrimitive(@Nullable Integer i) {
    // :: error: dereference
    return i == 1;
  }

  boolean equalToReference(@Nullable Integer i, Integer j) {
    return i == j;
  }

  boolean equalToNull(@Nullable Integer i) {
    return i == null;
  }

  boolean conditionalAnd(@Nullable Boolean b) {
    // :: error: dereference
    return b && true;
  }

  String concatenation(@Nullable Integer i) {
    return "i=" + i;
  }

  int unary(@Nullable Integer i) {
    // :: error: dereference
    return -i;
  }

  boolean not(@Nullable Boolean b) {
    // :: error: dereference
    return !b;
  }

  void increment(@Nullable Integer i) {
    // :: error: dereference
    i++;
  }

  void compoundAssignment(@Nullable Integer i) {
    int j = 0;
    // :: error: dereference
    j += i;
  }

  void compoundAssignmentToNullable(@Nullable Integer i) {
    // :: error: dereference
    i += 1;
  }

  void stringCompoundAssignment(@Nullable Integer i) {
    String s = "";
    s += i;
  }

  int conditional(@Nullable Boolean b) {
    // :: error: dereference
    return b ? 1 : 0;
  }

  void whileLoop(@Nullable Boolean b) {
    // :: error: dereference
    while (b) {}
  }

  void doWhileLoop(@Nullable Boolean b) {
    // :: error: dereference
    do {} while (b);
  }

  void forLoop(@Nullable Boolean b) {
    // :: error: dereference
    for (; b; ) {}
  }

  int arrayIndex(int[] a, @Nullable Integer i) {
    // :: error: dereference
    return a[i];
  }

  int[] arrayDimension(@Nullable Integer i) {
    // :: error: dereference
    return new int[i];
  }

  void switchStatement(@Nullable Integer i) {
    // :: error: dereference
    switch (i) {
      default:
    }
  }

  int switchExpression(@Nullable Integer i) {
    // :: error: dereference
    return switch (i) {
      default -> 0;
    };
  }

  void switchOnString(@Nullable String s) {
    // :: error: dereference
    switch (s) {
      default:
    }
  }

  int nonNull(Integer i, Boolean b, int[] a) {
    while (b) {
      i++;
    }
    return b ? a[i] + -i : new int[i].length;
  }

  int refined(@Nullable Integer i) {
    if (i == null) {
      return 0;
    }
    return i + 1;
  }
}
