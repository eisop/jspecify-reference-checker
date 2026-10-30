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
// is not a narrowing. These overrides were reported as override.receiver.invalid.

class UnmarkedGenericOverride<T> {
  @Override
  public String toString() {
    return "";
  }

  @Override
  public int hashCode(UnmarkedGenericOverride<T> this) {
    return 0;
  }

  class Inner {
    @Override
    public String toString() {
      return "";
    }
  }
}
