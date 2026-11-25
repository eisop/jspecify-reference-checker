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

// A null check can make a variable or field null-inclusive even if its declared type is
// null-exclusive: in the branch where `o == null` holds, `o` is null, and after that branch it may
// be null. The value after the check, not the declared type, decides whether a use is reported. So
// does the value of an assignment, even one that is itself reported. Comparing with a nullable
// value does not make a variable or field null-inclusive. See the samples inputs
// NullCheckUnrefineNonNullable.java, NullCheckTypeVariableUnrefineNonNullable.java, and
// AssignToCaught.java.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class NullCheckWidensVariable<T> {
  Object equalToNull(Object o) {
    if (o == null) {
      // :: error: return.type.incompatible
      return o;
    }
    return o;
  }

  Object notEqualToNull(Object o) {
    if (o != null) {
      return o;
    } else {
      // :: error: return.type.incompatible
      return o;
    }
  }

  Object typeVariable(T o) {
    if (o == null) {
      // :: error: return.type.incompatible
      return o;
    }
    return o;
  }

  Object local() {
    Object o = new Object();
    if (null == o) {
      // :: error: return.type.incompatible
      return o;
    }
    return o;
  }

  void log() {}

  Object afterNullBranch(Object o) {
    if (o == null) {
      log();
    }
    // :: error: return.type.incompatible
    return o;
  }

  Object afterThrow(Object o) {
    if (o == null) {
      throw new IllegalArgumentException();
    }
    return o;
  }

  Object afterReassignment(Object o) {
    if (o == null) {
      o = new Object();
    }
    return o;
  }

  <N extends @Nullable Object> N nullableBound(N n) {
    if (n != null) {
      n.hashCode();
    }
    return n;
  }

  <N extends @Nullable Object> Object nullableBoundInBranch(N n) {
    if (n == null) {
      // :: error: return.type.incompatible
      return n;
    }
    return n;
  }

  <N extends @Nullable Object> N nullableBoundAfterNullBranch(N n) {
    if (n == null) {
      log();
    }
    return n;
  }

  // A local variable's type is its value in the store, not the default for local variables.
  <N extends @Nullable Object> N nullableBoundLocal(N n) {
    N local = n;
    if (local != null) {
      local.hashCode();
    }
    return local;
  }

  <N extends @Nullable Object> N nullableBoundLocalAfterNullBranch(N n) {
    N local = n;
    if (local == null) {
      log();
    }
    return local;
  }

  <N extends @Nullable Object> N nullableBoundAssignedInCheck(N n) {
    N local;
    if ((local = n) == null) {
      log();
    }
    return local;
  }

  Object nullableRefined(@Nullable Object o) {
    o = new Object();
    if (o == null) {
      // :: error: return.type.incompatible
      return o;
    }
    return o;
  }

  Object nullable(@Nullable Object o) {
    if (o == null) {
      // :: error: return.type.incompatible
      return o;
    }
    return o;
  }

  void caught() {
    try {
      throw new Exception();
    } catch (Exception e) {
      e.toString();
      // :: error: assignment.type.incompatible
      e = null;
      // :: error: dereference
      e.toString();
    }
  }

  void caughtNullCheck() {
    try {
      throw new Exception();
    } catch (Exception e) {
      if (e == null) {
        // :: error: dereference
        e.toString();
      }
      // :: error: dereference
      e.toString();
    }
  }

  Object assignedNullable(Object o, @Nullable Object n) {
    // :: error: assignment.type.incompatible
    o = n;
    // :: error: return.type.incompatible
    return o;
  }

  Object negatedNotEqual(Object o) {
    if (!(o != null)) {
      // :: error: return.type.incompatible
      return o;
    }
    return o;
  }

  Object orNullOrEmpty(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    return s;
  }

  Object orNullOrEmptyAfterNullBranch(String s) {
    if (s == null || s.isEmpty()) {
      log();
    }
    // :: error: return.type.incompatible
    return s;
  }

  Object andBothNull(Object a, Object b) {
    if (a == null && b == null) {
      // :: error: return.type.incompatible
      return a;
    }
    // Even if the condition is false, `a` may be null, but `b` is not.
    b.hashCode();
    // :: error: return.type.incompatible
    return a;
  }

  Object conditionalExpression(Object o) {
    return o == null ? "" : o;
  }

  Object conditionalExpressionAfterNullBranch(Object o) {
    Object r = o == null ? "" : o;
    // :: error: return.type.incompatible
    return o;
  }

  Object patternVariable(Object o) {
    if (o instanceof String s && s == null) {
      // :: error: return.type.incompatible
      return s;
    }
    return o;
  }

  boolean cond() {
    return true;
  }

  // A null check later in a loop body widens the variable for the next iteration.
  Object loop(Object o) {
    while (cond()) {
      // :: error: dereference
      o.hashCode();
      if (o == null) {
        log();
      }
    }
    // :: error: return.type.incompatible
    return o;
  }

  Object loopContinue(Object o) {
    while (cond()) {
      if (o == null) {
        continue;
      }
      o.hashCode();
    }
    // :: error: return.type.incompatible
    return o;
  }

  Object loopThrow(Object o) {
    while (cond()) {
      if (o == null) {
        throw new IllegalArgumentException();
      }
      o.hashCode();
    }
    return o;
  }

  void lambda(Object o) {
    if (o == null) {
      // :: error: dereference
      Runnable r = () -> o.hashCode();
    }
  }

  void finallyBlock(Object o) {
    try {
      if (o == null) {
        throw new IllegalArgumentException();
      }
    } finally {
      // :: error: dereference
      o.hashCode();
    }
    o.hashCode();
  }

  void unboxing(Integer i) {
    if (i == null) {
      // :: error: dereference
      int j = i;
    }
  }

  // A catch parameter has no value in the store before it is compared.
  void caughtComparedWithNullable(@Nullable Object n) {
    try {
      throw new Exception();
    } catch (Exception e) {
      if (e == n) {
        e.toString();
      }
      e.toString();
    }
  }

  Object comparedWithNullable(Object o, @Nullable Object n) {
    if (o == n) {
      return o;
    }
    return o;
  }

  Object field = new Object();

  @Nullable Object nullableField;

  Object fieldEqualToNull() {
    if (field == null) {
      // :: error: return.type.incompatible
      return field;
    }
    return field;
  }

  Object fieldNotEqualToNull() {
    if (this.field != null) {
      return this.field;
    }
    // :: error: return.type.incompatible
    return this.field;
  }

  Object fieldAfterNullBranch() {
    if (field == null) {
      log();
    }
    // :: error: return.type.incompatible
    return field;
  }

  Object fieldAfterReassignment() {
    if (field == null) {
      field = new Object();
    }
    return field;
  }

  Object fieldOfOtherObject(NullCheckWidensVariable<T> other) {
    if (other.field == null) {
      // :: error: return.type.incompatible
      return other.field;
    }
    return other.field;
  }

  Object fieldAssignedNullable(@Nullable Object n) {
    // :: error: assignment.type.incompatible
    field = n;
    // :: error: return.type.incompatible
    return field;
  }

  @Nullable Object find() {
    return null;
  }

  Object fieldComparedWithNullable() {
    if (find() == field) {
      return field;
    }
    return field;
  }

  Object fieldComparedWithNullableField() {
    if (field == nullableField) {
      return field;
    }
    return field;
  }

  static class Holder<N extends @Nullable Object> {
    N value;

    Holder(N value) {
      this.value = value;
    }

    N nullableBound() {
      if (value != null) {
        value.hashCode();
      }
      return value;
    }

    Object nullableBoundInBranch() {
      if (value == null) {
        // :: error: return.type.incompatible
        return value;
      }
      return value;
    }
  }
}
