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

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests that illegal annotations on qualified types are reported whether or not the type is
 * parameterized. Outer-type qualifiers and annotations belong to the base type of a {@code
 * ParameterizedTypeTree} rather than the tree itself.
 */
@NullMarked
class TestAnnotationOnQualifiedType {
  class Inner {}

  class GenericInner<X> {}

  // :: error: outer.annotated
  @Nullable TestAnnotationOnQualifiedType.Inner outerAnnotated =
      new TestAnnotationOnQualifiedType().new Inner();

  // :: error: outer.annotated
  @Nullable TestAnnotationOnQualifiedType.GenericInner<String> outerAnnotatedParameterized =
      new TestAnnotationOnQualifiedType().new GenericInner<>();

  // :: error: outer.annotated
  @Nullable TestAnnotationOnQualifiedType.Inner outerAnnotatedReturn() {
    throw new RuntimeException();
  }

  // :: error: outer.annotated
  @Nullable TestAnnotationOnQualifiedType.GenericInner<String> outerAnnotatedParameterizedReturn() {
    throw new RuntimeException();
  }

  void locals() {
    // :: error: local.variable.annotated
    @Nullable String plain = "";
    // :: error: local.variable.annotated
    @Nullable List<String> parameterized = null;
    // :: error: local.variable.annotated
    java.util.@Nullable List raw = null;
    // :: error: local.variable.annotated
    java.util.@Nullable List<String> qualifiedParameterized = null;

    // A type argument's nullness is meaningful even for a local, so none of these is an error.
    List<@Nullable String> nullableElements = null;
    java.util.List<@Nullable String> qualifiedNullableElements = null;
    @Nullable String[] nullableComponents = null;

    used(plain, parameterized, raw, qualifiedParameterized);
    used(nullableElements, qualifiedNullableElements, nullableComponents);
  }

  static void used(@Nullable Object... values) {}
}
