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

// javac's capture conversion does not create a type variable for `? super Object` when the type
// parameter's bound is also `Object` (JDK-8054309). A `Lib<? super Object>` can still be a
// `Lib<@Nullable Object>`, so reading from it gives a nullable value, as for `Lib<? super String>`.
// See the samples input ContainmentSuperVsExtendsSameType.java.

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class SuperWildcardSameBound {
  interface Lib<T extends @Nullable Object> {
    T get();

    void put(T t);
  }

  static <T extends @Nullable Object> T first(Lib<T> lib) {
    return lib.get();
  }

  static <T extends @Nullable Object> void both(Lib<T> a, Lib<T> b) {}

  void read(Lib<? super Object> lib) {
    // :: error: dereference
    lib.get().toString();
    // :: error: dereference
    first(lib).toString();
  }

  Object readReturn(Lib<? super Object> lib) {
    // :: error: return.type.incompatible
    return lib.get();
  }

  void readSuperString(Lib<? super String> lib) {
    // :: error: dereference
    lib.get().toString();
  }

  void write(Lib<? super Object> lib) {
    lib.put(new Object());
    // :: error: argument.type.incompatible
    lib.put(null);
    both(lib, lib);
  }
}
