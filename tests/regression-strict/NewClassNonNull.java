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

// The result of a class instance creation is never null, also for an anonymous class in
// null-unmarked code.

import org.jspecify.annotations.NullMarked;

class NewClassNonNull {
  static class Unmarked {}

  Object field = new Object() {};

  void method() {
    Lib.take(new Object());
    Lib.take(new Unmarked());
    Lib.take(new Object() {});
    Lib.take(new Unmarked() {});
    Lib.take(
        new Runnable() {
          @Override
          public void run() {}
        });
  }

  @NullMarked
  static class Lib {
    static void take(Object o) {}
  }
}
