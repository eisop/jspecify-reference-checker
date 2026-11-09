// Copyright 2020 The JSpecify Authors
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

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;

final class NullSpecTypeValidator extends BaseTypeValidator {
  private final AnnotationMirror nullnessOperatorUnspecified;

  /** Constructor. */
  NullSpecTypeValidator(
      BaseTypeChecker checker,
      NullSpecVisitor visitor,
      NullSpecAnnotatedTypeFactory atypeFactory,
      Util util) {
    super(checker, visitor, atypeFactory);

    nullnessOperatorUnspecified = util.nullnessOperatorUnspecified;
  }

  /**
   * Makes an explicit nullness annotation on a type parameter, as in {@code <@Nullable T>}, inert:
   * javac puts it on the type parameter's implicit lower bound, so this removes the lower bound's
   * qualifier and re-defaults it. The mistake itself is reported by {@link
   * NullSpecVisitor#visitTypeParameter}; this only stops it from cascading into a spurious
   * bound-mismatch error.
   *
   * <p>Only the tree can tell an explicit annotation from the qualifier that defaulting puts on
   * every lower bound. None of our qualifiers has a {@code @TargetLocations}, so the superclass's
   * check would find nothing.
   */
  @Override
  protected void validateTypeParameterTargetLocations(AnnotatedTypeVariable type, Tree tree) {
    if (tree instanceof TypeParameterTree
        && NullSpecVisitor.hasExplicitNullnessAnnotation(
            ((TypeParameterTree) tree).getAnnotations())) {
      type.getLowerBound().clearAnnotations();
      atypeFactory.addDefaultAnnotations(type);
    }
  }

  /**
   * Wildcard analog of {@link #validateTypeParameterTargetLocations}: makes an explicit nullness
   * annotation on a wildcard, as in {@code @Nullable ?}, inert by re-defaulting the wildcard's
   * super bound, where javac puts it. The extends bound, as in {@code ? extends @Nullable Foo}, is
   * a recognized location and is left alone. The mistake is reported by {@link
   * NullSpecVisitor#visitAnnotatedType}.
   */
  @Override
  protected void validateWildcardTargetLocations(AnnotatedWildcardType type, Tree tree) {
    // A wildcard's tree is a type, never an expression, so it can never be parenthesized: `tree`
    // is already the AnnotatedTypeTree we need, if there is one.
    if (tree instanceof AnnotatedTypeTree
        && NullSpecVisitor.hasExplicitNullnessAnnotation(
            ((AnnotatedTypeTree) tree).getAnnotations())) {
      type.getSuperBound().clearAnnotations();
      atypeFactory.addDefaultAnnotations(type);
    }
  }

  @Override
  public boolean areBoundsValid(AnnotatedTypeMirror upperBound, AnnotatedTypeMirror lowerBound) {
    if (upperBound.hasAnnotation(nullnessOperatorUnspecified)
        || lowerBound.hasAnnotation(nullnessOperatorUnspecified)) {
      return true;
    } else {
      return super.areBoundsValid(upperBound, lowerBound);
    }
  }

  /**
   * Does nothing: skips the superclass's recheck that a captured wildcard's upper bound is a
   * subtype of the wildcard's extends bound.
   *
   * <p>That recheck is a tautology under ordinary capture conversion, but breaks under this
   * checker's "strict mode" subtyping, where {@code unspecified <: unspecified} is "not enough
   * information" -- so it spuriously rejects, e.g., {@code Lib<@NullnessUnspecified ?>} alongside
   * the intended {@code wildcard.annotated} error. The superclass's other checks are unaffected,
   * including its JDK-8054309 check.
   *
   * <p>TODO: this and {@link NullSpecVisitor#shouldCheckTypeArgument}'s wildcard-capture skip
   * together leave nothing checking a source-written wildcard's bound for containment in the type
   * parameter's declared bound (e.g. {@code Lib<? extends @Nullable Object>} against a
   * non-null-bounded {@code T}); needs a containment check against the written bound.
   */
  @Override
  protected void checkCapturedWildcardBounds(
      AnnotatedDeclaredType type, AnnotatedDeclaredType capturedType, ParameterizedTypeTree tree) {}
}
