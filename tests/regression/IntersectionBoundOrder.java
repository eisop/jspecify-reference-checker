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

// Every bound of an intersection constrains it, whichever position it is in: one non-null bound
// makes a type variable null-exclusive, and only nullable bounds make it null-inclusive.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class IntersectionBoundOrder {
  interface Lib {}

  interface Lib2 {}

  <T extends @Nullable Object & Lib> void nullableFirst(T t) {
    t.toString();
  }

  <T extends Lib & @Nullable Lib2> void nullableLast(T t) {
    t.toString();
  }

  <T extends @Nullable Lib & Lib2> void nullableFirstInterface(T t) {
    t.toString();
  }

  <T extends @Nullable Object & @Nullable Lib> void allNullable(T t) {
    // :: error: (dereference)
    t.toString();
  }

  <T extends @Nullable Object & Lib> T nullableFirstReturn(T t) {
    // :: error: (return.type.incompatible)
    return null;
  }

  <T extends @Nullable Object & @Nullable Lib> T allNullableReturn(T t) {
    // :: error: (return.type.incompatible)
    return null;
  }
}
