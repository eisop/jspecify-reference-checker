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

// The result of a class instance creation is never null, also for an anonymous class or a
// constructor reference in null-unmarked code.

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;

class NewClassNonNull {
  static class Unmarked {
    Unmarked() {}

    Unmarked(String s) {}
  }

  class Inner {}

  Object field = new Object() {};

  void method(NewClassNonNull outer) {
    Lib.take(new Object());
    Lib.take(new Unmarked());
    Lib.take(new Object() {});
    Lib.take(new Unmarked() {});
    Lib.take(
        new Runnable() {
          @Override
          public void run() {}
        });
    Lib.takeList(new ArrayList<>() {});
    Lib.take(outer.new Inner() {});
    new Object() {}.hashCode();
  }

  void constructorReference() {
    Lib.takeSupplier(Object::new);
    Lib.takeSupplier(Unmarked::new);
    Lib.takeFunction(Unmarked::new);
  }

  @NullMarked
  static class Lib {
    static void take(Object o) {}

    static void takeList(List<String> list) {}

    static void takeSupplier(Supplier<Object> supplier) {}

    static void takeFunction(Function<String, Object> function) {}
  }
}
