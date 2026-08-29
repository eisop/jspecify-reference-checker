// Copyright 2021 The JSpecify Authors
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

package com.google.jspecify.nullness;

import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;

final class NullSpecStore extends CFAbstractStore<CFValue, NullSpecStore> {
  NullSpecStore(NullSpecAnalysis analysis, boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
  }

  NullSpecStore(NullSpecStore other) {
    super(other);
  }

  /**
   * Stores a refinement for a nondeterministic expression -- a method call -- just as for a
   * deterministic one, ignoring the superclass's {@code permitNondeterministic} argument.
   *
   * <p>This checker deliberately refines method calls: {@code NullSpecTransfer} records what a
   * future {@code map.get(key)} will return from a preceding {@code map.containsKey(key)}, and
   * {@code if (foo.bar() != null) foo.bar().baz()} is expected to work. Neither is sound in general
   * -- an intervening call can invalidate either -- but rejecting them would be a false positive on
   * ordinary code.
   */
  @Override
  protected boolean shouldInsert(
      JavaExpression expr, CFValue value, boolean permitNondeterministic) {
    return super.shouldInsert(expr, value, /* permitNondeterministic= */ true);
  }
}
