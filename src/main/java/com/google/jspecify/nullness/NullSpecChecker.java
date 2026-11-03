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

import static com.sun.source.util.TaskEvent.Kind.COMPILATION;
import static javax.tools.Diagnostic.Kind.NOTE;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Log;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * Main entry point for a jspecify nullness checker.
 *
 * <p>Supported options:
 *
 * <ol>
 *   <li>"strict": Whether the checker should be a sound, strict type system. Does not imply that
 *       implementation code is checked.
 *   <li>"checkImpl": Whether implementation code should be checked.
 *   <li>"showTypes": Whether to output type information for the conformance test suite.
 * </ol>
 *
 * <p>Test harnesses also pass Checker Framework's {@code -AcheckCastElementType} (declared by
 * {@code SourceChecker}), which extends cast-safety checks to nested type arguments. It is not
 * enabled by default to avoid requiring newer Checker Framework versions for all users.
 */
@SupportedOptions({"strict", "checkImpl", "showTypes"})
public final class NullSpecChecker extends BaseTypeChecker {
  /*
   * A non-final field is ugly, but we can't create our Util instance in the constructor because the
   * ProcessingEnvironment isn't available then.
   *
   * But why make it a field at all, as opposed to a local variable in createSourceVisitor? The
   * problem is that NullSpecVisitor's constructor needs a way to access it through the checker
   * object. It needs that because the BaseTypeVisitor constructor (NullSpecVisitor's
   * superconstructor) calls createTypeFactory, and createTypeFactory needs access to util -- before
   * the NullSpecVisitor constructor has run. Thus, to access it, it has to be able to read it
   * through the BaseTypeVisitor.checker field, which *has* already been initialized. That means
   * reading it through this field.
   */
  Util util;

  boolean reportedNullnessError;

  public NullSpecChecker() {}

  @Override
  public NavigableSet<String> getSuppressWarningsPrefixes() {
    TreeSet<String> prefixes = new TreeSet<>();
    prefixes.add("nullness");
    return prefixes;
  }

  @Override
  protected String suppressWarningsString(String messageKey) {
    return "nullness";
  }

  /**
   * Returns true for a class annotated with {@code @SuppressWarnings("nullness")}, and for the
   * classes that CF itself skips.
   *
   * <p>We skip a class or method annotated with {@code @SuppressWarnings("nullness")} entirely.
   * This is in contrast to the default CF approach, which is to continue to process code but not
   * report warnings.
   *
   * <p>The advantage to our approach is that, if our checker crashes or takes a long time for a
   * given piece of code (perhaps especially <i>generated</i> code), users can easily disable
   * checking for that code.
   *
   * <p>Why does CF behave the way it does? I'm hoping that it's mostly because its infrastructure
   * doesn't want to make assumptions about what kinds of warnings a given checker can produce. We,
   * by contrast, have implemented {@link #getSuppressWarningsPrefixes()} to make <i>all</i> our
   * warnings suppressible with {@code @SuppressWarnings("nullness")}.
   *
   * <p>But I do worry a little that the checker needs to do <i>some</i> processing of a class tree
   * so that it can use that information when it checks <i>other</i> classes. If so, then skipping
   * it could be trouble. But I'm cautiously optimistic that, since we never visit <i>trees</i> at
   * all for <i>classpath</i> dependencies, we can get away without visiting them for <i>source</i>
   * dependencies, too.
   *
   * <p>{@code BaseTypeVisitor.visitClass} consults this method before the type factory's {@code
   * preProcessClassTree} (which runs dataflow), {@code processClassTree} and {@code
   * postProcessClassTree}, and {@code visitMethod} consults {@link #shouldSkipDefs(MethodTree)}
   * before {@code processMethodTree}. {@code NullSpecVisitor.visitVariable} also returns early for
   * a variable annotated with {@code @SuppressWarnings("nullness")}; CF has no such hook for
   * variables.
   *
   * @param tree a class
   * @return true if the class is not checked
   */
  @Override
  public boolean shouldSkipDefs(ClassTree tree) {
    return util.hasSuppressWarningsNullness(tree.getModifiers().getAnnotations())
        || super.shouldSkipDefs(tree);
  }

  /**
   * Returns true for a method annotated with {@code @SuppressWarnings("nullness")}, and for the
   * methods that CF itself skips. See {@link #shouldSkipDefs(ClassTree)} for why such a method is
   * skipped entirely.
   *
   * @param tree a method
   * @return true if the method is not checked
   */
  @Override
  public boolean shouldSkipDefs(MethodTree tree) {
    return util.hasSuppressWarningsNullness(tree.getModifiers().getAnnotations())
        || super.shouldSkipDefs(tree);
  }

  @Override
  public boolean shouldSuppressWarnings(Element elt, String errKey) {
    /*
     * Suppress CF's "conflicting.defaults" diagnostic. When @NullMarked and @NullUnmarked appear on
     * the same element, NullSpecVisitor.checkNoConflictingMarkingAnnotations already reports
     * "conflicting.annotations" at each annotation location, making CF's internal-vocabulary
     * diagnostic redundant.
     */
    return errKey.equals("conflicting.defaults") || super.shouldSuppressWarnings(elt, errKey);
  }

  @Override
  public void initChecker() {
    super.initChecker();

    JavacTask.instance(processingEnv)
        .addTaskListener(
            new TaskListener() {
              @Override
              public void finished(TaskEvent event) {
                if (reportedNullnessError && event.getKind() == COMPILATION) {
                  processingEnv
                      .getMessager()
                      .printMessage(
                          NOTE,
                          "\n"
                              + "=====\n"
                              + "The nullness checker is still early in development.\n"
                              + "It has many rough edges.\n"
                              + "For an introduction, see https://jspecify.dev/docs/user-guide\n"
                              + "=====");
                }
              }
            });
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    this.util = new Util(getElementUtils(), getTypeUtils()); // see discussion on the field
    return new NullSpecVisitor(this, util);
  }

  @Override
  public void typeProcess(TypeElement element, TreePath path) {
    Log log = Log.instance(((JavacProcessingEnvironment) processingEnv).getContext());
    int errorsBefore = log.nerrors;
    super.typeProcess(element, path);
    reportedNullnessError |= (log.nerrors > errorsBefore);
  }
}
