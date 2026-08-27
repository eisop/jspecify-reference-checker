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

// A `Class::cast` method reference returns a non-null value when its target function type supplies
// a non-null argument, and type-argument inference must see that. See the samples input
// StreamFilterIsInstanceAndMapCast.java.

import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class ClassCastMethodReference {
  interface Lib {
    void dereference();
  }

  // The stream's element type is narrowed to non-null by the `isInstance` filter, so inference must
  // choose `Stream<Lib>`, not `Stream<Lib?>`, for the `map` call.
  Stream<Lib> filterAndMap(Stream<@Nullable Object> s) {
    return s.filter(Lib.class::isInstance).map(Lib.class::cast);
  }

  void filterAndMapAndLambda(Stream<@Nullable Object> s) {
    s.filter(Lib.class::isInstance).map(Lib.class::cast).forEach(l -> l.dereference());
  }

  void filterAndMapAndMethodReference(Stream<@Nullable Object> s) {
    s.filter(Lib.class::isInstance).map(Lib.class::cast).forEach(Lib::dereference);
  }

  // The same narrowing applies without a stream in sight.
  Function<Object, Lib> asFunction() {
    return Lib.class::cast;
  }

  // But it must not apply when the argument can be null: `Class.cast` returns null for a null
  // input, so the method reference's return type stays nullable.
  Function<@Nullable Object, Lib> asFunctionOfNullable() {
    // :: error: (methodref.return.invalid)
    return Lib.class::cast;
  }

  Function<@Nullable Object, @Nullable Lib> asFunctionOfNullableToNullable() {
    return Lib.class::cast;
  }
}
