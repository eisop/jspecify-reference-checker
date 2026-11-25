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

// The checker narrows the return type of some methods: Arrays.copyOf and copyOfRange,
// Collection.toArray(), ExecutionException.getCause(), InvocationTargetException.getCause() and
// getTargetException(), Class.getEnumConstants() on an enum's class, and Map.getOrDefault with
// non-null values and default. A method reference to one of them must get the same return type as
// an invocation, whether its receiver is an expression or the first parameter of its function type.

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class SpecialCasedMethodReferences {
  enum E {
    A
  }

  BiFunction<String[], Integer, String[]> copyOf() {
    return Arrays::copyOf;
  }

  Supplier<Object[]> toArray(List<String> list) {
    return list::toArray;
  }

  Function<List<String>, Object[]> toArrayUnbound() {
    return List::toArray;
  }

  static class StringList extends ArrayList<String> {
    Supplier<Object[]> toArrayOfThis() {
      return this::toArray;
    }
  }

  // The elements can be null, so the array's can be too.
  Supplier<Object[]> toArrayOfNullable(List<@Nullable String> list) {
    // :: error: (methodref.return.invalid)
    return list::toArray;
  }

  Supplier<Throwable> executionExceptionGetCause(ExecutionException e) {
    return e::getCause;
  }

  // Type-argument inference sees the narrowed return type.
  Stream<Throwable> executionExceptionGetCauseUnbound(Stream<ExecutionException> s) {
    return s.map(ExecutionException::getCause);
  }

  Supplier<Throwable> invocationTargetExceptionGetCause(InvocationTargetException e) {
    return e::getCause;
  }

  Function<InvocationTargetException, Throwable> invocationTargetExceptionGetTargetException() {
    return InvocationTargetException::getTargetException;
  }

  Supplier<E[]> getEnumConstants(Class<E> c) {
    return c::getEnumConstants;
  }

  Function<Class<E>, E[]> getEnumConstantsUnbound() {
    return Class::getEnumConstants;
  }

  BiFunction<String, String, String> getOrDefault(Map<String, String> map) {
    return map::getOrDefault;
  }

  // The values can be null, so the result can be too.
  BiFunction<String, String, String> getOrDefaultOfNullable(Map<String, @Nullable String> map) {
    // :: error: (methodref.return.invalid)
    return map::getOrDefault;
  }

  // The default can be null, so the result can be too.
  BiFunction<String, @Nullable String, String> getOrDefaultWithNullableDefault(
      Map<String, String> map) {
    // :: error: (methodref.return.invalid)
    return map::getOrDefault;
  }
}
