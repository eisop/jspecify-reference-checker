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

// In unmarked code, every use of a type variable defaults to unspecified nullness, including the
// type arguments of a method's receiver. An override's receiver is still the type of "this", so it
// is not a narrowing. The overrides of Object's methods, with an implicit or an explicit receiver,
// in a class or in an inner class, were reported as override.receiver.invalid: Object has no type
// argument to compare the receiver's with. The overrides of a generic superclass's method, also of
// a null-marked one, are compared with the superclass's type argument, which is just as
// unspecified; they were not reported, and must stay so.

import org.jspecify.annotations.NullMarked;

class UnmarkedGenericOverride<T> {
  @Override
  public String toString() {
    return "";
  }

  @Override
  public int hashCode(UnmarkedGenericOverride<T> this) {
    return 0;
  }

  void m() {}

  class Inner {
    @Override
    public String toString() {
      return "";
    }

    @Override
    public int hashCode(Inner this) {
      return 0;
    }

    @Override
    public boolean equals(UnmarkedGenericOverride<T>.Inner this, Object o) {
      return false;
    }
  }
}

class UnmarkedGenericOverrideChild<E> extends UnmarkedGenericOverride<E> {
  @Override
  void m() {}
}

class UnmarkedGenericOverrideExplicitChild<E> extends UnmarkedGenericOverride<E> {
  @Override
  void m(UnmarkedGenericOverrideExplicitChild<E> this) {}
}

@NullMarked
class UnmarkedGenericOverrideMarkedParent<T> {
  void m() {}
}

class UnmarkedGenericOverrideUnmarkedChild<E> extends UnmarkedGenericOverrideMarkedParent<E> {
  @Override
  void m() {}
}
