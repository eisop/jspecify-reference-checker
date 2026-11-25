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

import java.util.ArrayList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests that a nullness annotation on a supertype in an extends or implements clause is reported
 * once: a supertype is always non-null.
 */
@NullMarked
class TestAnnotatedSupertype {
  // :: error: supertype.annotated
  static class ExtendsNullable extends @Nullable Object {}

  // :: error: supertype.annotated
  abstract static class ExtendsParameterized extends @Nullable ArrayList<String> {}

  // :: error: supertype.annotated
  abstract static class ImplementsParameterized implements @Nullable Comparable<String> {}

  // :: error: supertype.annotated
  abstract static class ImplementsSecond implements Runnable, @Nullable Comparable<String> {}

  // :: error: supertype.annotated
  interface InterfaceExtends extends @Nullable Runnable {}

  // :: error: supertype.annotated
  enum EnumImplements implements @Nullable Runnable {
    VALUE;

    @Override
    public void run() {}
  }

  // :: error: supertype.annotated
  record RecordImplements() implements @Nullable Runnable {
    @Override
    public void run() {}
  }

  // The annotation is on a type argument, not on the supertype.
  abstract static class NullableTypeArgument extends ArrayList<@Nullable String> {}
}
