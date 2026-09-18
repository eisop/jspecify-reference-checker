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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.framework.qual.TypeUseLocation;
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
   * Opts in to {@link BaseTypeValidator}'s strip-and-redefault handling for an invalid-location
   * type-variable or wildcard bound. Our {@link #additionalAnnotationsToStripFromTypeVariableBound}
   * and {@link #additionalAnnotationsToStripFromWildcardBound} overrides decide what (if anything)
   * to strip by inspecting the declaration's tree, since none of our qualifiers carries a
   * {@code @TargetLocations} for the superclass's own default detection to key off of. We don't
   * override {@code annotationsDisallowedAtLocation}: it also drives unconditional error reporting,
   * before the strip opt-in is even consulted.
   */
  @Override
  protected boolean shouldStripInvalidLocationQualifiers() {
    return true;
  }

  /**
   * Returns the qualifiers to strip from a type parameter's implicit lower bound, but only when
   * {@code tree} itself carries an explicit nullness annotation (as in {@code <@Nullable T>}) --
   * never merely because the bound's {@link AnnotatedTypeMirror} carries one of our qualifiers,
   * which it always does from ordinary defaulting. (Our qualifiers carry no
   * {@code @TargetLocations}, so the superclass's own default detection would find nothing to strip
   * and leave this a no-op; hence checking the tree directly instead.) The mistake itself is
   * reported elsewhere, by tree inspection; this only stops it from cascading into a further,
   * spurious bound-mismatch error.
   */
  @Override
  protected List<AnnotationMirror> additionalAnnotationsToStripFromTypeVariableBound(
      AnnotatedTypeVariable type, Tree tree, AnnotatedTypeMirror bound, TypeUseLocation location) {
    if (location != TypeUseLocation.LOWER_BOUND
        || !(tree instanceof TypeParameterTree)
        || !NullSpecVisitor.hasExplicitNullnessAnnotation(
            ((TypeParameterTree) tree).getAnnotations())) {
      return Collections.emptyList();
    }
    return supportedQualifiersOn(bound);
  }

  /**
   * Wildcard analog of {@link #additionalAnnotationsToStripFromTypeVariableBound}: strips from the
   * super bound only when the wildcard's own tree carries an explicit nullness annotation (as in
   * {@code @Nullable ?}). Never strips from the extends bound, which -- unlike a type parameter's
   * implicit lower bound -- is already a valid, always-recognized position on its own (as in {@code
   * ? extends @Nullable Foo}).
   */
  @Override
  protected List<AnnotationMirror> additionalAnnotationsToStripFromWildcardBound(
      AnnotatedWildcardType type,
      Tree tree,
      AnnotatedTypeMirror bound,
      Set<TypeUseLocation> allowedLocations) {
    // Distinguishes the super-bound call from the extends-bound call: only
    // WILDCARD_SUPER_BOUND_LOCATIONS contains LOWER_BOUND.
    if (!allowedLocations.contains(TypeUseLocation.LOWER_BOUND)) {
      return Collections.emptyList();
    }
    // A wildcard's bound tree is a type, never an expression, so unlike an ExpressionTree it can
    // never be parenthesized -- `tree` is already the AnnotatedTypeTree we need, if there is one.
    if (!(tree instanceof AnnotatedTypeTree)
        || !NullSpecVisitor.hasExplicitNullnessAnnotation(
            ((AnnotatedTypeTree) tree).getAnnotations())) {
      return Collections.emptyList();
    }
    return supportedQualifiersOn(bound);
  }

  /**
   * Returns {@code bound}'s primary annotations. (All of them are already qualifiers this checker
   * recognizes: {@link AnnotatedTypeMirror#addAnnotation} never stores an unsupported one.)
   */
  private List<AnnotationMirror> supportedQualifiersOn(AnnotatedTypeMirror bound) {
    return new ArrayList<>(bound.getAnnotations());
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
   * including the JDK-8054309 check below.
   *
   * <p>TODO: this and {@link NullSpecVisitor#shouldCheckTypeArgument}'s wildcard-capture skip
   * together leave nothing checking a source-written wildcard's bound for containment in the type
   * parameter's declared bound (e.g. {@code Lib<? extends @Nullable Object>} against a non-null-
   * bounded {@code T}); needs a containment check against the written bound.
   */
  @Override
  protected void checkCapturedWildcardBounds(
      AnnotatedDeclaredType type, AnnotatedDeclaredType capturedType, ParameterizedTypeTree tree) {}

  /**
   * Does nothing: skips the superclass's JDK-8054309 "collapsed wildcard" check, which has no
   * counterpart in the JSpecify specification.
   *
   * <p>The superclass reports {@code type.invalid.super.wildcard} when a wildcard's super bound is
   * the same base type as the type parameter's upper bound but differently annotated, because javac
   * then reuses the bound instead of creating a fresh type variable (<a
   * href="https://bugs.openjdk.org/browse/JDK-8054309">JDK-8054309</a>), losing its annotations.
   * That's a javac representation quirk, not a spec rule: per JLS 5.1.10 capture conversion (all
   * the spec adds to), the type converts to an ordinary, well-formed fresh type variable instead.
   *
   * <p>Skipping the check doesn't add unsoundness, just stops flagging that quirk; it does mean a
   * few reads (e.g. of {@code Lib<? super Object>}'s members) see the javac-collapsed, non-null
   * bound rather than the capture's true nullable one -- a gap that needs modeling the fresh type
   * variable javac declined to create (compare {@link #checkCapturedWildcardBounds} above).
   * Overriding {@code areCollapsedWildcardBoundsEqual} instead doesn't help either: our qualifier
   * hierarchy isn't antisymmetric, so its bidirectional-subtype test can't mean "same qualifier"
   * here.
   */
  @Override
  protected void checkExplicitSuperBoundWildcards(
      AnnotatedDeclaredType type, AnnotatedDeclaredType capturedType, ParameterizedTypeTree tree) {}
}
