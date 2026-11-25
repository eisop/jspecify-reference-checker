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

// The type of a cast to a wildcard-parameterized type must have the same captured type variables
// each time it is queried: type-argument inference and the argument check query it separately.
// Reduced from Guava's Maps.containsEntryImpl.

import java.util.Collection;
import java.util.Map.Entry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class CastCaptureInference {
  static <K extends @Nullable Object, V extends @Nullable Object> Entry<K, V> unmodifiable(
      Entry<? extends K, ? extends V> entry) {
    throw new UnsupportedOperationException();
  }

  boolean contains(Collection<Entry<String, String>> c, Object o) {
    return c.contains(unmodifiable((Entry<?, ?>) o));
  }
}
