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

// `this` is never null, also in field initializers and initializer blocks of null-unmarked code,
// where there is no receiver parameter to give it a type.

import org.jspecify.annotations.NullMarked;

class SelfTypeOutsideMethods {
  Object field = this;
  Object string = this.toString();

  {
    Lib.take(this);
  }

  @NullMarked
  static class Lib {
    static void take(Object o) {}
  }
}
