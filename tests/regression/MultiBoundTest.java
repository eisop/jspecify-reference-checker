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

import java.io.Serializable;
import java.io.Closeable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// Tests for multi-bound type variable handling.
// In NullMarked context, Serializable and Closeable are non-null by default.
// T extends @Nullable Serializable & Closeable: Closeable is non-null so T must be non-null.
@NullMarked
class MultiBoundTest {
  // T extends @Nullable Serializable & Closeable: Closeable (non-null) establishes T as non-null.
  // Returning T as Object! (non-null) should be FINE - no error.
  <T extends @Nullable Serializable & Closeable> Object nullableFirstNonNullSecond(T t) {
    return t; // should NOT be an error
  }

  // T extends Closeable & @Nullable Serializable: Closeable (non-null) establishes T as non-null.
  // Returning T as Object! should be FINE - no error.
  <T extends Closeable & @Nullable Serializable> Object nonNullFirstNullableSecond(T t) {
    return t; // should NOT be an error
  }

  // T extends @Nullable Serializable & @Nullable Closeable: both nullable - T can be null.
  // Returning T as Object! should be an error.
  <T extends @Nullable Serializable & @Nullable Closeable> Object bothNullable(T t) {
    // :: error: jspecify_nullness_mismatch
    return t;
  }

  // T extends Serializable & Closeable: both non-null (default in NullMarked).
  // Returning T as Object! should be FINE - no error.
  <T extends Serializable & Closeable> Object bothNonNull(T t) {
    return t; // should NOT be an error
  }
}
