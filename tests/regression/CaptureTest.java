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

// Tests for captured wildcard nullness handling.
// In NullMarked code, a captured wildcard is nullable iff ALL its effective upper bounds are
// nullable. The checker should detect when a nullable captured value is returned as non-null.
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class CaptureTest {
  // T extends @Nullable Foo, wildcard is unbounded (?).
  // In NullMarked, ? defaults to @Nullable, so F_cap has nullable upper bound -> error.
  Foo nullableUnbounded(NullableFooSupplier<?> supplier) {
    // :: error: return.type.incompatible
    return supplier.get();
  }

  // T extends @Nullable Object, wildcard is ? extends @Nullable Lib.
  // ALL upper bounds are nullable (@Nullable Lib, @Nullable Object) -> T_cap is nullable -> error.
  Object nullableExplicitBound(NullableBounded<? extends @Nullable Lib> x) {
    // :: error: return.type.incompatible
    return x.get();
  }

  // T extends @Nullable Object, wildcard is ? extends Lib (non-null).
  // Lib is non-null -> T_cap has non-null bound -> no error.
  Object nonNullExplicitBound(NullableBounded<? extends Lib> x) {
    return x.get(); // should NOT be an error
  }

  interface NullableFooSupplier<F extends @Nullable Foo> {
    F get();
  }

  interface NullableBounded<T extends @Nullable Object> {
    T get();
  }

  interface Foo {}
  interface Lib {}
}
