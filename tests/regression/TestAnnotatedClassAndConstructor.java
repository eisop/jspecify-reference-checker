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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests that nullness annotations on type declarations and on constructors are reported: neither is
 * a recognized location for a type-use annotation.
 */
@NullMarked
class TestAnnotatedClassAndConstructor {
  // :: error: type.declaration.annotated
  @Nullable static class NullableClass {}

  // :: error: type.declaration.annotated
  @NonNull interface NonNullInterface {}

  // :: error: type.declaration.annotated
  @Nullable enum NullableEnum {}

  // :: error: type.declaration.annotated
  @Nullable record NullableRecord() {}

  static class Constructors {
    // :: error: constructor.annotated
    @Nullable Constructors() {}

    // :: error: constructor.annotated
    @NonNull Constructors(int i) {}
  }
}
