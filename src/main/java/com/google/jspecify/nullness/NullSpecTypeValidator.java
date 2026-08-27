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
   * Opts in to {@link BaseTypeValidator}'s strip-and-redefault handling of type-variable and
   * wildcard bounds that carry a qualifier disallowed at their location. This opt-in is meaningful
   * only together with our overrides of {@link #additionalAnnotationsToStripFromTypeVariableBound}
   * and {@link #additionalAnnotationsToStripFromWildcardBound}, which decide -- by inspecting the
   * declaration's tree via {@link NullSpecVisitor#hasExplicitNullnessAnnotation}, not the derived
   * {@link AnnotatedTypeMirror} -- whether there is actually anything to strip. We deliberately do
   * not override {@code annotationsDisallowedAtLocation}: its {@code @TargetLocations}-based result
   * also drives unconditional error reporting in the superclass, before the strip opt-in is even
   * consulted, and none of our qualifiers carries {@code @TargetLocations} in the first place
   * (every one of them legitimately appears at every location through defaulting).
   */
  @Override
  protected boolean shouldStripInvalidLocationQualifiers() {
    return true;
  }

  /**
   * Returns the qualifiers to strip from a type parameter's implicit lower bound, and re-default,
   * only when {@code tree} itself carries an explicit JSpecify nullness annotation written directly
   * on the type parameter (as in {@code <@Nullable T>}) -- never merely because the lower bound's
   * {@link AnnotatedTypeMirror} happens to carry one of our qualifiers, which it always does, from
   * ordinary defaulting, whether or not the user wrote anything invalid.
   *
   * <p>This checker's qualifiers carry no {@code @TargetLocations} -- every one of them
   * legitimately appears at every location through defaulting, as {@link
   * #shouldStripInvalidLocationQualifiers} explains -- so the superclass's default detection, which
   * finds nothing to strip without {@code @TargetLocations}, would leave this method a no-op.
   * Consulting {@code tree} directly, instead, correctly distinguishes a user's explicit mistake
   * from the identical qualifier arriving at the same bound through defaulting. The explicit
   * mistake itself is reported elsewhere, by tree inspection; this method only prevents it from
   * cascading into a further, spurious bound-mismatch error.
   *
   * @param type the type-variable declaration whose bounds are being validated
   * @param tree the tree for {@code type}'s declaration
   * @param bound {@code type}'s upper or lower bound
   * @param location {@link TypeUseLocation#UPPER_BOUND} or {@link TypeUseLocation#LOWER_BOUND}
   * @return the qualifiers to strip from {@code bound}, or an empty list
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
   * Returns the qualifiers to strip from a wildcard's super bound when the wildcard's own tree
   * carries an explicit JSpecify nullness annotation written directly on the {@code ?} (as in
   * {@code @Nullable ?}), the wildcard analog of {@link
   * #additionalAnnotationsToStripFromTypeVariableBound} above. Never strips from the extends bound:
   * unlike a type parameter's identity (which maps to its implicit lower bound), a wildcard's
   * extends bound is a separate, always-recognized position (<code>
   * ? extends @Nullable Foo</code> is valid), so there is nothing to strip there.
   *
   * @param type the wildcard type being validated
   * @param tree the tree for {@code type}
   * @param bound {@code type}'s super or extends bound
   * @param allowedLocations the type-use locations {@code bound} may be annotated at; distinguishes
   *     the super-bound call from the extends-bound call, since {@code
   *     BaseTypeValidator#WILDCARD_SUPER_BOUND_LOCATIONS} is the only one of the two sets that
   *     contains {@link TypeUseLocation#LOWER_BOUND}
   * @return the qualifiers to strip from {@code bound}, or an empty list
   */
  @Override
  protected List<AnnotationMirror> additionalAnnotationsToStripFromWildcardBound(
      AnnotatedWildcardType type,
      Tree tree,
      AnnotatedTypeMirror bound,
      Set<TypeUseLocation> allowedLocations) {
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
   * Does nothing, skipping the superclass's recheck that each captured wildcard's upper bound is a
   * subtype of the wildcard's extends bound.
   *
   * <p>That recheck is a tautology by the construction of capture conversion: the capture's upper
   * bound is the greatest lower bound of the wildcard's extends bound and the type parameter's
   * bound, so it is a subtype of both. Under this checker's nonstandard subtyping the tautology
   * breaks: in "strict mode," {@code unspecified <: unspecified} is "not enough information," so
   * the recheck rejects, for example, {@code Lib<@NullnessUnspecified ?>} with a spurious {@code
   * type.argument.type.incompatible} in addition to the intended {@code wildcard.annotated} error.
   * The samples expect no such diagnostics, so skip the recheck.
   *
   * <p>The superclass's other checks are unaffected: {@code checkTypeArguments} runs (and applies
   * the wildcard-capture skip in {@link NullSpecVisitor#shouldCheckTypeArgument}), and the
   * JDK-8054309 check in {@code checkExplicitSuperBoundWildcards} still reports {@code
   * type.invalid.super.wildcard}.
   *
   * <p>TODO: with this recheck and {@link NullSpecVisitor#shouldCheckTypeArgument}'s wildcard-
   * capture skip both in place, nothing checks a source-written wildcard's bound for containment in
   * the corresponding type parameter's declared bound (e.g. {@code Lib<? extends @Nullable Object>}
   * against a non-null-bounded {@code T} in null-marked code). Neither skip is what loses the
   * check: the capture's own upper bound (the greatest lower bound of the wildcard's extends bound
   * and the type parameter's bound) comes out non-null, so neither recheck would have caught the
   * violation anyway. Catching it needs a containment check against the written bound.
   *
   * @param type the (possibly unconverted) parameterized type being validated
   * @param capturedType {@code type} after capture conversion
   * @param tree the tree for {@code type}
   */
  @Override
  protected void checkCapturedWildcardBounds(
      AnnotatedDeclaredType type, AnnotatedDeclaredType capturedType, ParameterizedTypeTree tree) {}

  /**
   * Does nothing, skipping the superclass's JDK-8054309 "collapsed wildcard" check, which has no
   * counterpart in the JSpecify specification.
   *
   * <p>The superclass reports {@code type.invalid.super.wildcard} when a wildcard's explicit super
   * bound is the same base type as the type parameter's declared upper bound but carries different
   * annotations, because javac then reuses the bound instead of creating a fresh type variable (see
   * <a href="https://bugs.openjdk.org/browse/JDK-8054309">JDK-8054309</a>) and the extends bound's
   * annotations are lost. That is a report about javac's -- and hence the Checker Framework's --
   * representation, not about the program.
   *
   * <p>The specification has no such rule. Its "Capture conversion" section says only "The Java
   * rules are defined in [JLS 5.1.10]. We add to them as follows," and every addition it lists
   * concerns nullness operators; nothing reuses a declared bound in place of a fresh type variable.
   * So under the spec, {@code Lib<? super Object>} against {@code Lib<T extends @Nullable Object>}
   * capture-converts by JLS 5.1.10 verbatim, to a fresh variable with lower bound {@code Object}
   * and upper bound {@code Object UNION_NULL} -- an ordinary, well-formed type. JDK-8054309 is a
   * javac bug, and the spec defines no notion of a wildcard being invalid because its two bounds'
   * nullness differs. Accordingly the samples treat these types as unremarkable: {@code
   * SuperObject}, {@code SuperObjectUnspec}, and {@code SuperObjectUnionNull} are one systematic
   * family, with expected facts only on the calls, never on the declaration.
   *
   * <p>Suppressing the report does not introduce unsoundness; it only stops announcing one that is
   * already present and that the report's wording does not describe. Because javac hands us the
   * collapsed type, this checker reads a {@code T}-returning member of {@code Lib<? super Object>}
   * as non-null rather than as the capture's {@code Object UNION_NULL} upper bound, and for the
   * same reason it misses the expected mismatch on {@code
   * ContainmentSuperVsExtendsSameType.java:23}, where containment compares the collapsed non-null
   * {@code Object} instead of that upper bound. Fixing those requires modeling the fresh type
   * variable that javac declined to create, which is out of this validator's reach; compare the
   * separate, similarly documented gap on {@link #checkCapturedWildcardBounds} above.
   *
   * <p>{@code areCollapsedWildcardBoundsEqual} is the superclass hook for adjusting how the two
   * bounds are compared, and it is what a checker like this one -- whose qualifier hierarchy is not
   * antisymmetric, so the superclass's bidirectional-subtype test cannot mean "same qualifier" here
   * -- would need if it wanted the check at all. This checker wants it not at all, so it skips the
   * enclosing method instead.
   *
   * @param type the (possibly unconverted) parameterized type being validated
   * @param capturedType {@code type} after capture conversion
   * @param tree the tree for {@code type}
   */
  @Override
  protected void checkExplicitSuperBoundWildcards(
      AnnotatedDeclaredType type, AnnotatedDeclaredType capturedType, ParameterizedTypeTree tree) {}
}
