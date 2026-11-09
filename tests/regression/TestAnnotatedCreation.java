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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests that a nullness annotation on an object creation or on the array of an array creation is
 * reported, even where the result may be null: the result of a creation is always non-null.
 */
@NullMarked
class TestAnnotatedCreation {
  class Inner {}

  void objects(TestAnnotatedCreation outer) {
    // :: error: new.object.annotated
    new @Nullable Object();
    // :: error: new.object.annotated
    new @NonNull Object();
    // :: error: new.object.annotated
    new java.util.@Nullable ArrayList<String>();
    // :: error: new.object.annotated
    outer.new @Nullable Inner();
    // :: error: new.object.annotated
    new @Nullable Object() {};
    // :: error: new.object.annotated
    new @Nullable Runnable() {
      @Override
      public void run() {}
    };
    // The annotation is on a type argument.
    new ArrayList<@Nullable String>();
  }

  // :: error: new.array.annotated
  Object @Nullable [] dimension = new Object @Nullable [1];

  // :: error: new.array.annotated
  Object @Nullable [] initializer = new Object @Nullable [] {};

  // :: error: new.array.annotated
  Object @Nullable [] @Nullable [] multidimensional = new Object @Nullable [1] @Nullable [1];

  // The annotations are on component types.
  @Nullable Object[] component = new @Nullable Object[1];

  Object[] @Nullable [] innerArray = new Object[1] @Nullable [];
}
