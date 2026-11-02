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

// A null check can make a variable null-inclusive even if its declared type is null-exclusive: in
// the branch where `o == null` holds, `o` is null. The variable's value after the check, not its
// declared type, decides whether a use is reported. So does the value of an assignment, even one
// that is itself reported. See the samples inputs NullCheckUnrefineNonNullable.java,
// NullCheckTypeVariableUnrefineNonNullable.java, and AssignToCaught.java.

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
}
