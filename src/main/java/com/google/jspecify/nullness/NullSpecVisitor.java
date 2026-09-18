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

import static com.google.jspecify.nullness.Util.IMPLEMENTATION_VARIABLE_KINDS;
import static com.google.jspecify.nullness.Util.nameMatches;
import static com.sun.source.tree.Tree.Kind.EXTENDS_WILDCARD;
import static com.sun.source.tree.Tree.Kind.PRIMITIVE_TYPE;
import static com.sun.source.tree.Tree.Kind.SUPER_WILDCARD;
import static com.sun.source.tree.Tree.Kind.UNBOUNDED_WILDCARD;
import static java.util.Arrays.asList;
import static javax.lang.model.element.ElementKind.ENUM_CONSTANT;
import static javax.lang.model.element.ElementKind.PACKAGE;
import static org.checkerframework.framework.util.AnnotatedTypes.asSuper;
import static org.checkerframework.javacutil.AnnotationUtils.annotationName;
import static org.checkerframework.javacutil.AnnotationUtils.areSameByName;
import static org.checkerframework.javacutil.TreeUtils.annotationFromAnnotationTree;
import static org.checkerframework.javacutil.TreeUtils.annotationsFromTypeAnnotationTrees;
import static org.checkerframework.javacutil.TreeUtils.elementFromDeclaration;
import static org.checkerframework.javacutil.TreeUtils.elementFromTree;
import static org.checkerframework.javacutil.TreeUtils.elementFromUse;
import static org.checkerframework.javacutil.TypesUtils.isPrimitive;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

final class NullSpecVisitor extends BaseTypeVisitor<NullSpecAnnotatedTypeFactory> {
  private final boolean checkImpl;
  private final Util util;

  NullSpecVisitor(NullSpecChecker checker, Util util) {
    super(checker);
    this.util = util;
    checkImpl = checker.hasOption("checkImpl");
  }

  /**
   * Returns whether any of {@code annotations} is one of this checker's three nullness annotations,
   * as written directly in source.
   *
   * <p>Unlike almost all other logic in this checker, which operates on qualifiers' internal
   * representation in an {@link AnnotatedTypeMirror}, this inspects the syntax tree directly. That
   * distinction matters here specifically because our qualifiers are this checker's entire type
   * system: every type variable's implicit lower bound and every wildcard's implicit super bound
   * already carries one of them from ordinary defaulting, whether or not the user wrote anything at
   * all. Only the tree can distinguish a real, explicit annotation from that default.
   */
  static boolean hasExplicitNullnessAnnotation(List<? extends AnnotationTree> annotations) {
    for (AnnotationMirror annotation : annotationsFromTypeAnnotationTrees(annotations)) {
      if (isNullnessAnnotation(annotation)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNullnessAnnotation(AnnotationMirror annotation) {
    return NULLNESS_ANNOTATIONS.contains(annotationName(annotation));
  }

  private void ensureNonNull(Tree tree) {
    ensureNonNull(tree, /* messageKey= */ "dereference");
  }

  private void ensureNonNull(Tree tree, String messageKey) {
    AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(tree);
    // Maybe this should call isSubtype(type, objectMinusNull)? I'd need to create objectMinusNull.
    if (!isPrimitive(type.getUnderlyingType())
        && !atypeFactory.isNullExclusiveUnderEveryParameterization(type)) {
      String origin = originString(tree);
      checker.reportError(tree, messageKey, type + (origin.isEmpty() ? "" : ", " + origin));
    }
  }

  /* TODO: implement feature to add extra args to return type errors.
   *
  @Override
  protected String extraArgForReturnTypeError(Tree tree) {
    /
     * We call originStringIfTernary, not originString:
     *
     * If the statement is `return foo.bar()`, then the problem is obvious, so we don't want our
     * error message longer to restate "the problem is foo.bar()."
     *
     * But if the statement is `return b ? foo.bar() : baz`, then the problem may be more subtle, so
     * we want to give more details.
     *
     * TODO(cpovirk): Further improve this to call attention to *which* of the 2 branches produces
     * the possibly null value (possibly both!). However, this gets tricky: If the branches return
     * `Foo?` and `Foo*`, then we ideally want to emphasize the `Foo?` branch *but*, at least in
     * "strict mode," not altogether ignore the `Foo*` branch.
      /
    String origin = originStringIfTernary(tree);
    return origin.isEmpty() ? "" : (origin + "\n");
  }
  */

  private String originString(Tree tree) {
    while (tree instanceof ParenthesizedTree) {
      tree = ((ParenthesizedTree) tree).getExpression();
    }
    if (tree instanceof MethodInvocationTree) {
      ExecutableElement method = elementFromUse((MethodInvocationTree) tree);
      return "returned from "
          + method.getEnclosingElement().getSimpleName()
          + "."
          + method.getSimpleName();
    }
    return originStringIfTernary(tree);
  }

  private String originStringIfTernary(Tree tree) {
    while (tree instanceof ParenthesizedTree) {
      tree = ((ParenthesizedTree) tree).getExpression();
    }
    if (tree instanceof ConditionalExpressionTree) {
      ConditionalExpressionTree ternary = (ConditionalExpressionTree) tree;
      ExpressionTree trueExpression = ternary.getTrueExpression();
      ExpressionTree falseExpression = ternary.getFalseExpression();
      AnnotatedTypeMirror trueType = atypeFactory.getAnnotatedType(trueExpression);
      AnnotatedTypeMirror falseType = atypeFactory.getAnnotatedType(falseExpression);
      String trueOrigin = originString(trueExpression);
      String falseOrigin = originString(falseExpression);

      return "result of ternary operator on "
          + trueType
          + (trueOrigin.isEmpty() ? "" : (" (" + trueOrigin + ")"))
          + " and "
          + falseType
          + (falseOrigin.isEmpty() ? "" : (" (" + falseOrigin + ")"));
    }
    return "";
  }

  @Override
  public Void visitBlock(BlockTree tree, Void p) {
    if (checkImpl) {
      return super.visitBlock(tree, p);
    } else {
      // TODO(cpovirk): Should we still check any classes inside the block (e.g., anonymous)?
      return null;
    }
  }

  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    // TODO: ensure no explicit annotations on class & constructor
  }

  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree tree,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    /*
     * Skip the check that requires receivers to be null-exclusive. I *believe* this is redundant
     * with the check in visitMemberSelect. And the check in visitMemberSelect both covers more
     * cases (including field lookups) and provides a more tailored error message.
     */
    return true;
  }

  /**
   * Returns true if the given member select tree's expression is a "true expression." This is in
   * contrast to when it is class or package name: Those can appear as an "expresion" in a member
   * select, but they do not represent true expressions.
   */
  private static boolean memberSelectExpressionIsATrueExpression(MemberSelectTree memberSelect) {
    if (memberSelect.getIdentifier().contentEquals("class")) {
      // This case is largely covered by the checks below, but it's still necessary for void.class.
      return false;
    }
    Element element = elementFromTree(memberSelect.getExpression());
    if (element == null) {
      // Some expressions do not have an associated symbol -- for example, ternaries.
      return true;
    }
    return !element.getKind().isClass()
        && !element.getKind().isInterface()
        && element.getKind() != PACKAGE;
    /*
     * If a class/interface/package appears as the "expression" of a member select, then we're
     * looking at a case like `Foo.Baz`, `Foo<Bar>.Baz`, or `java.util.List`. Our checks want to
     * distinguish this from the case in which the member select represents an actual dereference.
     *
     * As it happens, it's not only *unnecessary* to check that an "expression" like "Foo" is
     * non-null, but it's also currently *unsafe*: The "expression" tree's nullness can default to
     * NullnessUnspecified (based on some combination of whether *Foo* is not @NullMarked and the
     * *usage* is not @NullMarked). Thus, anything that looks like dereference of the "expression"
     * could produce a warning or error.
     *
     * Note that our defaulting of enclosing types in
     * writeDefaultsForIntrinsicallyNonNullableComponents does not help. It does not help even when
     * I retrieve the type of the entire MemberSelectTree (and then pull out the outer type from
     * that).
     *
     * The code path that we end up in appears to be looking specifically at the class referenced by
     * the MemberSelectTree, without regard to any annotations on, e.g., the VariableTree that it is
     * the type for. We end up in AnnotatedTypeFactory.fromElement. Possibly that's bogus: Not every
     * MemberSelectTree is an "expression" in the usual sense. Perhaps it's our job not to call
     * getAnnotatedType on such trees? So let's not.
     */
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, Void p) {
    ExpressionTree expression = tree.getExpression();
    if (memberSelectExpressionIsATrueExpression(tree)) {
      ensureNonNull(expression);
    }
    /*
     * visitMemberSelect has to look for annotations differently than visitVariable and visitMethod.
     *
     * In those other cases, a @Nullable type annotation on the outer type of `Outer.Inner` appears
     * in the parse tree as an annotation on the variable/method modifiers. (See JLS 9.7.4, which in
     * turn references 8.3.) Thus, if the method return type is *any* member-select tree, we know we
     * have a return type of the form `@Nullable Foo.Bar`.
     *
     * In this case, we already know that we have some kind of member select, and so we want to look
     * for annotations on the "left side" of the select (if that side turns out to be a type rather
     * than, say, an instance, like in `foo.bar()`).
     *
     * I would not be at all surprised if there are additional cases that we still haven't covered.
     *
     * In all cases in which we report outer.annotated, we know that we're dealing with a true inner
     * class (`@Nullable Outer.Inner`), not a static nested class (`@Nullable Map.Entry`). That's
     * because the latter is rejected by javac itself ("scoping construct cannot be annotated with
     * type-use annotation"). Thus, it's safe for our message to speak specifically about the
     * inner-class case.
     */
    checkNoNullnessAnnotationsOnType(tree, expression, "outer.annotated");
    return super.visitMemberSelect(tree, p);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    ensureNonNull(tree.getExpression());
    return super.visitEnhancedForLoop(tree, p);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, Void p) {
    ensureNonNull(tree.getExpression());
    return super.visitArrayAccess(tree, p);
  }

  @Override
  protected void checkThrownExpression(ThrowTree tree) {
    ensureNonNull(tree.getExpression());
  }

  @Override
  public Void visitSynchronized(SynchronizedTree tree, Void p) {
    ensureNonNull(tree.getExpression());
    return super.visitSynchronized(tree, p);
  }

  @Override
  public Void visitAssert(AssertTree tree, Void p) {
    ensureNonNull(tree.getCondition());
    if (tree.getDetail() != null) {
      ensureNonNull(tree.getDetail());
    }
    return super.visitAssert(tree, p);
  }

  @Override
  public Void visitIf(IfTree tree, Void p) {
    ensureNonNull(tree.getCondition());
    return super.visitIf(tree, p);
  }

  // TODO: binary, unary, compoundassign, typecast, ...

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    ExecutableElement executable = elementFromUse(tree);

    checkForAtomicReferenceConstructorCall(tree, executable);

    return super.visitMethodInvocation(tree, p);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, Void p) {
    ExecutableElement constructor = elementFromUse(tree);

    checkForAtomicReferenceConstructorCall(tree, constructor);

    /*
     * TODO(cpovirk): Figure out whether to report this here (at the instantiation) or at the
     * declaration of the problem class.
     *
     * In some ways, reporting at the declaration of the problem class makes more sense: If someone
     * defines `final class MyThreadLocal extends ThreadLocal<String>` without overriding
     * initialValue(), then that class is not a valid implementation of ThreadLocal<String>, and
     * there's nothing the caller can do -- not subclass it, not assign it to a
     * ThreadLocal<@Nullable String> instead. So perhaps MyThreadLocal should have been required to
     * be at least an extensible class -- and perhaps ideally even to redefine initialValue() as
     * abstract.
     *
     * On the other hand, we might not analyze MyThreadLocal at all, so we'd fail to report an error
     * for code that we know is dangerous. But is that really any different than the normal dangers
     * of unanalyzed code? It just happens to be one case in which we can catch the problem anyway.
     *
     * In practice, it probably matters little: Most ThreadLocal classes are likely to be used
     * nearby -- including the specific case of an anonymous ThreadLocal implementation.
     */
    TypeElement clazz = (TypeElement) constructor.getEnclosingElement();
    if (types.isSubtype(
            types.erasure(clazz.asType()), types.erasure(util.javaLangThreadLocalElement.asType()))
        && !overridesInitialValue(clazz)) {
      AnnotatedDeclaredType annotatedType = atypeFactory.getAnnotatedType(tree);
      AnnotatedDeclaredType annotatedTypeAsThreadLocal =
          asSuper(atypeFactory, annotatedType, atypeFactory.javaLangThreadLocal);
      AnnotatedTypeMirror typeArg = annotatedTypeAsThreadLocal.getTypeArguments().get(0);
      if (!atypeFactory.isNullInclusiveUnderEveryParameterization(typeArg)) {
        checker.reportError(tree, "threadlocal.must.include.null", typeArg);
      }
    }
    return super.visitNewClass(tree, p);
  }

  private void checkForAtomicReferenceConstructorCall(
      ExpressionTree tree, ExecutableElement constructor) {
    // TODO(cpovirk): Also check AtomicReferenceArray.
    if (nameMatches(constructor, "AtomicReference", "<init>")
        && constructor.getParameters().isEmpty()) {
      AnnotatedTypeMirror typeArg =
          ((AnnotatedDeclaredType) atypeFactory.getAnnotatedType(tree)).getTypeArguments().get(0);
      if (!atypeFactory.isNullInclusiveUnderEveryParameterization(typeArg)) {
        checker.reportError(tree, "atomicreference.must.include.null", typeArg);
      }
    }
  }

  /*
   * TODO(cpovirk): Also check AtomicReference and ThreadLocal value types for null-inclusiveness
   * when users create instances by using method references (e.g., AtomicReference::new).
   */

  private boolean overridesInitialValue(TypeElement clazz) {
    // ThreadLocal.initialValue() can be absent if we're running with j2cl's limited classpath.
    if (!util.threadLocalInitialValueElement.isPresent()) {
      return false;
    }
    ExecutableElement initialValue = util.threadLocalInitialValueElement.get();
    Elements elements = atypeFactory.getElementUtils();
    return ElementUtils.getAllMethodsIn(clazz, elements).stream()
        .anyMatch(method -> elements.overrides(method, initialValue, clazz));
  }

  /*
   * We perform our checks for errors of the form "X should not be annotated" here. This requires
   * some gymnastics to deal with annotations on arrays, etc. and with the general problem that
   * visitAnnotatedType is not invoked in many cases we might like for it to be invoked. For
   * example, it doesn't run on an annotated return type. That's because the visit* methods are
   * triggered based on javac tree structure, and the return type's annotations get attached to the
   * *method* tree.
   *
   * It would be appealing to instead perform the checks in an implementation of
   * isTopLevelValidType. However, this has several downsides. In short, we need to use methods like
   * visitTypeParameter and visitAnnotatedType because we want to operate on source trees, rather
   * than on derived types. For more details, see the bullet points below, plus the additional notes
   * in the commit message of
   * https://github.com/jspecify/jspecify-reference-checker/commit/611717437c3c8b967de6d21615223975dd97b60a
   *
   * - If we instead want to look for annotations on a type parameter or wildcard based on the
   * derived types, we need to ask CF questions like "What is the lower/upper bound?" since that is
   * what CF translates such annotations into. That then requires us to carefully distinguish
   * between implicit upper bounds (like the upper bound of `? super Foo`) and explicit upper bounds
   * (like the upper bound of `@Nullable ? super Foo`). This is likely to be clumsy at best,
   * requiring us to effectively look at information in the source code, anyway -- if sufficient
   * information is even available, especially across compilation boundaries!
   *
   * - We might also like that the visit* methods can check specifically for the JSpecify
   * annotations. This means that people can alias annotations like CF's own @Nullable to ours, and
   * this checker won't produce errors if they're using in non-JSpecify-recognized locations. (On
   * the other hand, some users might *want* us to produce warnings in such cases so that they are
   * informed that they're stepping outside of core JSpecify semantics.)
   */

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, Void p) {
    checkNoNullnessAnnotations(tree, tree.getAnnotations(), "type.parameter.annotated");
    return super.visitTypeParameter(tree, p);
  }

  @Override
  public Void visitAnnotatedType(AnnotatedTypeTree tree, Void p) {
    List<? extends AnnotationTree> annotations = tree.getAnnotations();
    Kind kind = tree.getUnderlyingType().getKind();
    if (kind == UNBOUNDED_WILDCARD || kind == EXTENDS_WILDCARD || kind == SUPER_WILDCARD) {
      checkNoNullnessAnnotations(tree, annotations, "wildcard.annotated");
    } else if (kind == PRIMITIVE_TYPE) {
      checkNoNullnessAnnotations(tree, annotations, "primitive.annotated");
    }
    return super.visitAnnotatedType(tree, p);
  }

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    // For discussion of short-circuiting, see processClassTree.
    List<? extends AnnotationTree> annotations = tree.getModifiers().getAnnotations();
    if (util.hasSuppressWarningsNullness(annotations)) {
      return null;
    }

    if (isPrimitiveOrArrayOfPrimitive(tree.getType())) {
      checkNoNullnessAnnotations(tree, annotations, "primitive.annotated");
    } else if (baseTypeTree(tree.getType()) instanceof MemberSelectTree) {
      checkNoNullnessAnnotations(tree, annotations, "outer.annotated");
    }

    ElementKind kind = elementFromDeclaration(tree).getKind();
    if (kind == ENUM_CONSTANT) {
      checkNoNullnessAnnotations(tree, annotations, "enum.constant.annotated");
    } else if (IMPLEMENTATION_VARIABLE_KINDS.contains(kind)) {
      /*
       * A local's type-use annotation appears on its modifiers when written first (`@Nullable
       * String s`, per JLS 9.7.4), or on the type tree when preceded by a package or outer-type
       * qualifier (`java.util.@Nullable List<String> s`). Check both.
       *
       * (Element.getAnnotationMirrors does not report type-use annotations. Array annotations are
       * reached through component types and handled separately below.)
       */
      if (tree.getType() instanceof ArrayTypeTree) {
        checkNoNullnessAnnotationsOnArrayItself(tree, "local.variable.annotated");
      } else {
        checkNoNullnessAnnotations(tree, annotations, "local.variable.annotated");
        checkNoNullnessAnnotationsOnType(tree, tree.getType(), "local.variable.annotated");
      }
    }
    return super.visitVariable(tree, p);
  }

  @Override
  public void processMethodTree(String className, MethodTree tree) {
    // For discussion of short-circuiting, see processClassTree.
    List<? extends AnnotationTree> annotations = tree.getModifiers().getAnnotations();
    if (util.hasSuppressWarningsNullness(annotations)) {
      return;
    }

    Tree returnType = tree.getReturnType();
    if (returnType != null) {
      if (isPrimitiveOrArrayOfPrimitive(returnType)) {
        checkNoNullnessAnnotations(tree, annotations, "primitive.annotated");
      } else if (baseTypeTree(returnType) instanceof MemberSelectTree) {
        checkNoNullnessAnnotations(tree, annotations, "outer.annotated");
      }
    } else {
      /*
       * A constructor has no return type, but an annotation can still be written in that syntactic
       * position (`@Nullable Foo() {}`), and javac attaches it to the method's own modifiers since
       * there's no return-type tree to hang it on. It's meaningless there either way.
       */
      checkNoNullnessAnnotations(tree, annotations, "constructor.annotated");
    }
    /*
     * Thrown types aren't a recognized location for type-use annotations (a checked exception's
     * nullness is not meaningful: you can't catch or throw a null reference). Like wildcard and
     * type-parameter declarations, nothing about visitAnnotatedType's generic tree-scanning
     * catches this: the annotated type in a throws clause has a perfectly ordinary Kind (e.g.
     * IDENTIFIER for `throws @Nullable Exception`), so it falls through
     * visitAnnotatedType's kind-specific checks unless we look for it here explicitly.
     */
    for (ExpressionTree thrownType : tree.getThrows()) {
      checkNoNullnessAnnotationsOnType(tree, thrownType, "exception.type.annotated");
    }
    /*
     * A receiver parameter (`void foo(Foo this)`) is always non-null -- like an enum constant, its
     * nullness can't meaningfully vary. It carries ElementKind.PARAMETER, the same kind as an
     * ordinary parameter, so it can't be picked out by kind the way visitVariable's
     * IMPLEMENTATION_VARIABLE_KINDS branch picks out locals: that would wrongly flag every
     * ordinary annotated parameter too. MethodTree exposes it directly, so check it here instead.
     */
    VariableTree receiverParameter = tree.getReceiverParameter();
    if (receiverParameter != null) {
      /*
       * Unlike an ordinary variable, a receiver parameter's type-use annotation isn't always
       * wrapped in an AnnotatedTypeTree around its type. Verified by inspection:
       *
       * - For a simple (unqualified) receiver type, e.g. `@Nullable Foo this`, getType() is a
       *   plain JCIdent, and the annotation shows up in getModifiers() instead.
       * - For a receiver type qualified by an enclosing class name, e.g. a nested class's
       *   `Outer.@Nullable Inner this` (annotating the receiver's own type) or an inner class
       *   constructor's `@Nullable Outer Outer.this` (its implicit enclosing-instance
       *   parameter), getType() *is* an AnnotatedTypeTree as usual, and getModifiers() is empty.
       *
       * So check both; each is empty except in the shape it actually applies to.
       */
      checkNoNullnessAnnotations(
          tree, receiverParameter.getModifiers().getAnnotations(), "receiver.annotated");
      checkNoNullnessAnnotationsOnType(tree, receiverParameter.getType(), "receiver.annotated");
    }
    checkNoConflictingMarkingAnnotations(annotations);
    super.processMethodTree(className, tree);
  }

  @Override
  public void processClassTree(ClassTree tree) {
    /*
     * We short-circuit if we see @SuppressWarnings("nullness"). This is in contrast to the default
     * CF approach, which is to continue to process code but not report warnings.
     *
     * The advantage to our approach is that, if our checker crashes or takes a long time for a
     * given piece of code (perhaps especially *generated* code), users can easily disable checking
     * for that code.
     *
     * Why does CF behave the way it does? I'm hoping that it's mostly because its infrastructure
     * doesn't want to make assumptions about what kinds of warnings a given checker can produce.
     * We, by contrast, have implemented NullSpecChecker.getSuppressWarningsPrefixes() to make *all*
     * our warnings suppressible with @SuppressWarnings("nullness").
     *
     * But I do worry a little that the checker needs to do *some* processing of a class tree so
     * that it can use that information when it checks *other* classes. If so, then our overrides of
     * processClassTree here and preProcessClassTree in NullSpecAnnotatedTypeFactory could be
     * trouble. But I'm cautiously optimistic that, since we never visit *trees* at all for
     * *classpath* dependencies, we can get away without visiting them for *source* dependencies,
     * too.
     *
     * Finally: For class-level suppression specifically, we can't override visitClass, which is
     * `final`. visitClass's docs advise us to override processClassTree. But that is not sufficient
     * to skip all expensive work, so we must also override preProcessClassTree in
     * NullSpecAnnotatedTypeFactory.
     */
    if (util.hasSuppressWarningsNullness(tree.getModifiers().getAnnotations())) {
      return;
    }

    checkNoConflictingMarkingAnnotations(tree.getModifiers().getAnnotations());

    /*
     * A supertype reference in an extends or implements clause is always non-null -- you can't
     * extend or implement a possibly-null type. Nothing else visits these: they're not a
     * variable's or method's type, so visitVariable/processMethodTree never see them, and their
     * Kind is as ordinary as any other type-use (e.g. IDENTIFIER), so visitAnnotatedType's
     * wildcard/primitive-specific branches don't either.
     */
    checkSupertypeClauseNotAnnotated(tree.getExtendsClause());
    for (Tree implementsClause : tree.getImplementsClause()) {
      checkSupertypeClauseNotAnnotated(implementsClause);
    }

    checkUninitializedFields(tree);

    super.processClassTree(tree);
  }

  /**
   * Reports {@code supertype.annotated} if {@code clause} (an {@code extends} or {@code implements}
   * clause, or {@code null} if the class has no {@code extends} clause) carries a nullness
   * annotation.
   */
  private void checkSupertypeClauseNotAnnotated(@Nullable Tree clause) {
    if (clause != null) {
      checkNoNullnessAnnotationsOnType(clause, clause, "supertype.annotated");
    }
  }

  /**
   * Reports an error on any instance field that has no initializer at its declaration, is never
   * assigned in any constructor or instance initializer block, and whose type isn't definitely
   * nullable -- so the implicit {@code null} the JVM gives it is incompatible with what the field
   * claims to hold.
   *
   * <p>This is a purely syntactic check, not real initialization analysis: "assigned before
   * construction completes" means some constructor's body, or some instance initializer block
   * (which the JLS has javac insert into every constructor right after the {@code super()} call),
   * contains a plain assignment statement to the field <em>on {@code this}</em> somewhere in its
   * text, regardless of whether every control-flow path actually reaches it. That's deliberately
   * coarser than sound initialization tracking (which would need a real dataflow subchecker) but
   * catches the common case of a field with no initializer anywhere at all.
   *
   * <p>Only the declaring class's own members are scanned, so a non-private field of an abstract
   * class that every concrete subclass assigns in its own constructor is still reported here. That
   * matches what the class on its own guarantees, but it leaves {@code @SuppressWarnings} as the
   * only escape hatch for that pattern.
   *
   * <p>Skips records entirely: a record component becomes a field with no declaration initializer,
   * assigned by the canonical constructor, but that assignment is textually absent from an implicit
   * canonical constructor (not present in the tree at all) and from a compact canonical
   * constructor's body (javac appends the field stores after it) -- so this scan's "textual
   * assignment somewhere in a constructor" heuristic can't see it and would report a false positive
   * on every record with a non-nullable component and no explicit canonical constructor. The JLS
   * forbids a record from declaring any instance field other than its components, so there is
   * nothing else here to check.
   *
   * <p>TODO: Cover static fields too, scanning static initializer blocks (also members of
   * ClassTree, distinguishable from instance initializer blocks via BlockTree.isStatic()) the same
   * way we scan constructors here. Left out for now: unlike a constructor, a static field is
   * commonly left without either a declaration initializer or a static-block assignment,
   * initialized lazily later by a factory or setter method instead -- a real pattern this text-only
   * scan can't distinguish from a genuine gap, so covering it needs more thought about false
   * positives, not just the mechanical addition of another TreeScanner pass.
   */
  private void checkUninitializedFields(ClassTree tree) {
    if (TreeUtils.isRecordTree(tree)) {
      return;
    }
    List<VariableTree> uninitializedFields = new ArrayList<>();
    for (Tree member : tree.getMembers()) {
      if (member instanceof VariableTree) {
        VariableTree field = (VariableTree) member;
        if (field.getInitializer() == null
            && !field.getModifiers().getFlags().contains(Modifier.STATIC)
            && !isPrimitiveOrArrayOfPrimitive(field.getType())) {
          uninitializedFields.add(field);
        }
      }
    }
    if (uninitializedFields.isEmpty()) {
      return;
    }

    Set<Element> fieldsAssignedBeforeConstructionCompletes = new HashSet<>();
    TreeScanner<Void, Void> assignmentScanner =
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitAssignment(AssignmentTree assignmentTree, Void p) {
            ExpressionTree lhs = assignmentTree.getVariable();
            /*
             * Only an assignment to the instance under construction counts. `other.field = ...`
             * assigns the same *element* on a different instance, so counting it would let a
             * constructor that never touches its own field pass.
             */
            if (lhs instanceof IdentifierTree
                || (lhs instanceof MemberSelectTree
                    && TreeUtils.isExplicitThisDereference(
                        ((MemberSelectTree) lhs).getExpression()))) {
              fieldsAssignedBeforeConstructionCompletes.add(elementFromUse(lhs));
            }
            return super.visitAssignment(assignmentTree, p);
          }
        };
    for (Tree member : tree.getMembers()) {
      if (member instanceof MethodTree && TreeUtils.isConstructor((MethodTree) member)) {
        BlockTree body = ((MethodTree) member).getBody();
        if (body != null) {
          body.accept(assignmentScanner, null);
        }
      } else if (member instanceof BlockTree && !((BlockTree) member).isStatic()) {
        // An instance initializer block: the JLS has javac insert its content into every
        // constructor right after the super() call, so an assignment there is exactly as
        // reliable as one directly in a constructor's own body.
        member.accept(assignmentScanner, null);
      }
    }

    for (VariableTree field : uninitializedFields) {
      if (fieldsAssignedBeforeConstructionCompletes.contains(elementFromDeclaration(field))) {
        continue;
      }
      AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(field);
      if (!atypeFactory.isNullInclusiveUnderEveryParameterization(fieldType)) {
        checker.reportError(field, "field.uninitialized");
      }
    }
  }

  private boolean isPrimitiveOrArrayOfPrimitive(Tree type) {
    return type instanceof PrimitiveTypeTree
        || (type instanceof ArrayTypeTree
            && isPrimitiveOrArrayOfPrimitive(((ArrayTypeTree) type).getType()));
  }

  private void checkNoNullnessAnnotationsOnArrayItself(
      VariableTree treeToReportOn, String messageKey) {
    Tree tree = treeToReportOn.getType();
    while (tree instanceof ArrayTypeTree) {
      tree = ((ArrayTypeTree) tree).getType();
    }
    checkNoNullnessAnnotationsOnType(treeToReportOn, tree, messageKey);
  }

  /**
   * Returns the tree that carries {@code type}'s own annotations and determines its own shape. For
   * a parameterized type like {@code Foo<Bar>}, that is the {@code Foo} part of the {@link
   * ParameterizedTypeTree}, not the {@link ParameterizedTypeTree} itself: both an annotation on the
   * type ({@code @Nullable Foo<Bar>}) and the qualified-name shape that distinguishes an outer type
   * ({@code Outer.Inner<Bar>}) live there.
   */
  private static Tree baseTypeTree(@Nullable Tree type) {
    return type instanceof ParameterizedTypeTree ? ((ParameterizedTypeTree) type).getType() : type;
  }

  /**
   * Reports {@code messageKey} for each nullness annotation written directly on {@code type}.
   *
   * @param treeToReportOn the tree to report on, which need not be {@code type} itself
   * @param type the type tree to inspect; nothing is reported if it is null or carries no
   *     annotations of its own
   * @param messageKey the message key to report
   */
  private void checkNoNullnessAnnotationsOnType(
      Tree treeToReportOn, @Nullable Tree type, String messageKey) {
    type = baseTypeTree(type);
    if (type instanceof AnnotatedTypeTree) {
      checkNoNullnessAnnotations(
          treeToReportOn, ((AnnotatedTypeTree) type).getAnnotations(), messageKey);
    }
  }

  private void checkNoNullnessAnnotations(
      Tree treeToReportOn, List<? extends AnnotationTree> annotations, String messageKey) {
    for (AnnotationMirror annotation : annotationsFromTypeAnnotationTrees(annotations)) {
      /*
       * TODO(cpovirk): Check for aliases here (and perhaps elsewhere).
       *
       * For now, we look only for our specific classes. Because we're looking at Tree instances, we
       * must look for the classes in the JSpecify annotations jar, not the ones that we use to
       * represent the types internally in AnnotatedTypeMirror instances. Contrast this to almost
       * all other logic in the checker, which operates on the internal types.
       */
      if (NULLNESS_ANNOTATIONS.stream().anyMatch(na -> areSameByName(annotation, na))) {
        checker.reportError(treeToReportOn, messageKey, annotationName(annotation));
      }
    }
  }

  private static final Set<String> NULLNESS_ANNOTATIONS =
      Set.of(
          "org.jspecify.annotations.NonNull",
          "org.jspecify.annotations.Nullable",
          "org.jspecify.annotations.NullnessUnspecified");

  /*
   * @NullMarked and @NullUnmarked are declaration annotations, each aliased (in
   * NullSpecAnnotatedTypeFactory) to a different DefaultQualifier.List. CF's generic
   * "conflicting annotations" check (BaseTypeValidator.isTopLevelValidType) only looks at
   * *type-use* annotations on a validated AnnotatedTypeMirror, so it never notices that a single
   * element carries both aliases. We look for the conflict directly in the declaration
   * annotations, the same way checkNoNullnessAnnotations looks for JSpecify's type-use
   * annotations in source rather than through the alias machinery.
   */
  private static final Set<String> MARKING_ANNOTATIONS =
      Set.of("org.jspecify.annotations.NullMarked", "org.jspecify.annotations.NullUnmarked");

  private void checkNoConflictingMarkingAnnotations(List<? extends AnnotationTree> annotations) {
    List<AnnotationTree> markingAnnotationTrees = new ArrayList<>();
    for (AnnotationTree annotationTree : annotations) {
      if (MARKING_ANNOTATIONS.contains(
          annotationName(annotationFromAnnotationTree(annotationTree)))) {
        markingAnnotationTrees.add(annotationTree);
      }
    }
    if (markingAnnotationTrees.size() > 1) {
      // Report each annotation at its own position (rather than at treeToReportOn's, e.g. the
      // enclosing class or method's), matching where the conformance tests' expected-fact
      // comments are anchored: on the line of each conflicting annotation itself.
      for (AnnotationTree annotationTree : markingAnnotationTrees) {
        checker.reportError(
            annotationTree,
            "conflicting.annotations",
            annotationName(annotationFromAnnotationTree(annotationTree)));
      }
    }
  }

  @Override
  protected boolean checkMethodReferenceAsOverride(MemberReferenceTree tree, Void p) {
    /*
     * Class.cast accepts `Object?`, so there's no need to check its parameter type: It can accept
     * nullable and non-nullable values alike.
     *
     * It returns `T?`, so we normally do need to check its _return type_ to see if that fits the
     * required type. *But*, if its parameter type is non-nullable, then so too is its return type.
     * And if its return type is non-nullable, then it returns a value that works whether we need a
     * nullable or non-nullable value. In that case, we can skip the superclass's checks entirely.
     *
     * This all relies on the fact that CF can infer the return type as non-nullable in the first
     * place. That does not happen on its own: this check runs only after type-argument inference,
     * so for a call like `stream.map(Foo.class::cast)`, whose element type is exactly what
     * inference is trying to determine, inference sees the declared `T?` return type and never
     * gets far enough to reach this check. NullSpecAnnotatedTypeFactory
     * .narrowClassCastReturnTypeIfArgumentIsNonNull applies the same reasoning as this method
     * where the method reference's type is first computed, which is early enough for inference to
     * benefit from it.
     */
    return atypeFactory.isClassCastAppliedToNonNullableType(tree)
        || super.checkMethodReferenceAsOverride(tree, p);
  }

  @Override
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return new AnnotationMirrorSet(asList(AnnotationBuilder.fromClass(elements, MinusNull.class)));
  }

  @Override
  protected NullSpecAnnotatedTypeFactory createTypeFactory() {
    // Reading util this way is ugly but necessary. See discussion in NullSpecChecker.
    return new NullSpecAnnotatedTypeFactory(checker, ((NullSpecChecker) checker).util);
  }

  @Override
  protected TypeValidator createTypeValidator() {
    return new NullSpecTypeValidator(checker, this, atypeFactory, ((NullSpecChecker) checker).util);
  }

  /**
   * Returns an {@link OverrideChecker} enforcing parameter invariance: parameters may neither be
   * widened (CF's contravariant default) nor narrowed.
   *
   * <p>Type-parameter bounds on generic methods need no custom override rules: CF's default {@code
   * isTypeParameterBoundOverrideValid} already reports {@code override.typaram.invalid} correctly
   * via this checker's {@link #typeHierarchy}.
   */
  @Override
  protected OverrideChecker createOverrideChecker(
      Tree overriderTree,
      AnnotatedExecutableType overrider,
      AnnotatedTypeMirror overriderType,
      AnnotatedTypeMirror overriderReturnType,
      AnnotatedExecutableType overridden,
      AnnotatedDeclaredType overriddenType,
      AnnotatedTypeMirror overriddenReturnType) {
    return new InvariantParameterOverrideChecker(
        overriderTree,
        overrider,
        overriderType,
        overriderReturnType,
        overridden,
        overriddenType,
        overriddenReturnType);
  }

  /**
   * Enforces parameter invariance by requiring {@code overriderParam} to be a subtype of {@code
   * capturedOverriddenParam}, in addition to {@link OverrideChecker#isParameterOverrideValid}'s
   * contravariant check. Skipped for method references, which have no declared parameter types of
   * their own to compare.
   */
  private final class InvariantParameterOverrideChecker extends OverrideChecker {
    InvariantParameterOverrideChecker(
        Tree overriderTree,
        AnnotatedExecutableType overrider,
        AnnotatedTypeMirror overriderType,
        AnnotatedTypeMirror overriderReturnType,
        AnnotatedExecutableType overridden,
        AnnotatedDeclaredType overriddenType,
        AnnotatedTypeMirror overriddenReturnType) {
      super(
          overriderTree,
          overrider,
          overriderType,
          overriderReturnType,
          overridden,
          overriddenType,
          overriddenReturnType);
    }

    @Override
    protected boolean isParameterOverrideValid(
        AnnotatedTypeMirror capturedOverriddenParam,
        AnnotatedTypeMirror overriddenParam,
        AnnotatedTypeMirror overriderParam) {
      if (!super.isParameterOverrideValid(
          capturedOverriddenParam, overriddenParam, overriderParam)) {
        return false;
      }
      if (isMethodReference) {
        return true;
      }
      return typeHierarchy.isSubtype(overriderParam, capturedOverriddenParam)
          || testTypevarContainment(overriderParam, overriddenParam);
    }
  }
}
