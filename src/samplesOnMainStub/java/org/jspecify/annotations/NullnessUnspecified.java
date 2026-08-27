// Copyright 2018-2020 The JSpecify Authors.
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

package org.jspecify.annotations;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stand-in for the {@code org.jspecify.annotations.NullnessUnspecified} that only the
 * samples-google-prototype-eisop fork of jspecify/jspecify defines, not the real {@code main}
 * branch. Several files under {@code ../jspecify/samples} import it (see the "Disclaimers" section
 * of {@code samples/README.md}: "we haven't yet removed the @NullnessUnspecified usages"), so
 * without this stand-in, compiling the samples in a single javac invocation against {@code main}'s
 * jspecify.jar fails on an unresolvable symbol and cascades into spurious errors across the whole
 * corpus, not just the files that use the annotation directly.
 *
 * <p>This is on {@code conformanceTests}' classpath only, not {@code jspecifySamplesTest}'s (where
 * the fork branch's own jspecify.jar already provides the real class) or {@code test}'s (whose
 * regression tests intentionally avoid this fork-only annotation so they compile against real
 * JSpecify). Content matches the fork's definition exactly, so the checker's existing
 * `addAliasedTypeAnnotation("org.jspecify.annotations.NullnessUnspecified", ...)` gives it the same
 * meaning either way.
 */
@Documented
@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface NullnessUnspecified {}
