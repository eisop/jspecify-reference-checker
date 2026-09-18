// Copyright 2022 The JSpecify Authors
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

package tests;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.jspecify.nullness.NullSpecChecker;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.checkerframework.framework.test.diagnostics.DetailedTestDiagnostic;
import org.checkerframework.framework.test.diagnostics.DiagnosticKind;
import org.checkerframework.framework.test.diagnostics.TestDiagnostic;
import org.jspecify.annotations.Nullable;
import org.jspecify.conformance.ConformanceTestRunner;
import org.jspecify.conformance.ExpectedFact;
import org.jspecify.conformance.ReportedFact;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Conformance tests for the JSpecify reference checker.
 *
 * <p>To configure:
 *
 * <ul>
 *   <li>Set the system property {@code JSpecifyConformanceTest.tests} to the location of the
 *       JSpecify conformance test sources.
 *   <li>Set the system property {@code JSpecifyConformanceTest.deps} to a colon-separated list of
 *       JARs that must be on the classpath when analyzing the JSpecify conformance test sources.
 *   <li>Set the sytem property {@code JSpecifyConformanceTest.report} to the location of the stored
 *       test report.
 *   <li>Do the same, but for {@code JSpecifyConformanceTest.samples.tests} and {@code
 *       JSpecifyConformanceTest.samples.report}, for running the conformance tests on the JSpecify
 *       samples directory.
 * </ul>
 *
 * <p>The test can run in one of three modes, depending on the value of the {@code
 * JSPECIFY_CONFORMANCE_TEST_MODE} environment variable:
 *
 * <dl>
 *   <dt>{@code compare} or empty
 *   <dd>Compare the analysis to the stored report, and fail if anything has changed. Note that
 *       failure isn't always bad! If an assertion used to fail and now passes, this test will
 *       "fail".
 *   <dt>{@code write}
 *   <dd>Write the analysis to the report file. Always passes.
 *   <dt>{@code details}
 *   <dd>Fail if any assertion fails. Report details of unexpected facts.
 * </dl>
 */
@RunWith(JUnit4.class)
public final class ConformanceTest {
  private static final ImmutableList<String> OPTIONS =
      ImmutableList.of(
          "-AassumePure",
          "-Adetailedmsgtext",
          "-AcheckImpl",
          "-AsuppressWarnings=conditional",
          "-Astrict",
          "-AshowTypes");

  private static final ImmutableList<Path> TEST_DEPS =
      Stream.ofNullable(System.getProperty("JSpecifyConformanceTest.deps"))
          .flatMap(Splitter.on(':').trimResults().omitEmptyStrings()::splitToStream)
          .map(Paths::get)
          .collect(toImmutableList());

  private final ConformanceTestRunner conformanceTestRunner =
      new ConformanceTestRunner(ConformanceTest::analyze);

  @Test
  public void conformanceTests() throws IOException {
    conformanceTestRunner.checkConformance(testDirectory(null), TEST_DEPS, testReport(null));
  }

  @Test
  public void conformanceTestsOnSamples() throws IOException {
    conformanceTestRunner.checkConformance(
        testDirectory("samples"), ImmutableList.of(), testReport("samples"));
  }

  private static Path testDirectory(@Nullable String prefix) {
    return systemPropertyPath(
        PROPERTY_JOINER.join("JSpecifyConformanceTest", prefix, "inputs"),
        "the location of the JSpecify conformance test inputs");
  }

  private static Path testReport(@Nullable String prefix) {
    return systemPropertyPath(
        PROPERTY_JOINER.join("JSpecifyConformanceTest", prefix, "report"),
        "the location of the JSpecify conformance test report");
  }

  private static final Joiner PROPERTY_JOINER = Joiner.on('.').skipNulls();

  private static Path systemPropertyPath(String key, String description) {
    return Paths.get(
        requireNonNull(
            System.getProperty(key),
            String.format("Set system property %s to %s.", key, description)));
  }

  private static ImmutableSet<ReportedFact> analyze(
      Path testDirectory, ImmutableSortedSet<Path> files, ImmutableList<Path> testDeps) {
    TestConfiguration config =
        TestConfigurationBuilder.buildDefaultConfiguration(
            null,
            files.stream().map(Path::toFile).collect(toImmutableSet()),
            testDeps.stream().map(Path::toString).collect(toImmutableList()),
            ImmutableList.of(NullSpecChecker.class.getName()),
            OPTIONS,
            TestUtilities.getShouldEmitDebugInfo());
    TypecheckResult result = new TypecheckExecutor().runTest(config);
    return result.getUnexpectedDiagnostics().stream()
        .map(d -> new DetailMessageReportedFact(testDirectory, d))
        .collect(toImmutableSet());
  }

  /** A {@link ReportedFact} parsed from a Checker Framework {@link DetailMessage}. */
  static final class DetailMessageReportedFact extends ReportedFact {

    private static final String DEREFERENCE = "dereference";

    // Shared with NullSpecTest.CANNOT_CONVERT_MESSAGE_KEYS_EXCEPT_DEREFERENCE -- see that field's
    // Javadoc for why the two harnesses read the same list instead of each keeping its own copy.
    private static final ImmutableSet<String> CANNOT_CONVERT_KEYS =
        NullSpecTest.CANNOT_CONVERT_MESSAGE_KEYS_EXCEPT_DEREFERENCE;

    private static final ImmutableSet<String> IRRELEVANT_ANNOTATION_KEYS =
        ImmutableSet.of(
            "conflicting.annotations",
            "exception.type.annotated",
            "local.variable.annotated",
            "primitive.annotated",
            "type.parameter.annotated",
            "wildcard.annotated");

    /**
     * The message key that CF's own {@code BaseTypeValidator.isTopLevelValidType} uses when a type
     * use carries more than one annotation from our nullness hierarchy (e.g.
     * {@code @Nullable @NonNull String}). Unlike our own {@code conflicting.annotations} checks
     * (see {@link
     * com.google.jspecify.nullness.NullSpecVisitor#checkNoConflictingMarkingAnnotations}), this
     * diagnostic comes from CF's generic machinery, which by the time it runs has already resolved
     * the JSpecify annotations to their internal, aliased qualifiers -- so its arguments name the
     * internal qualifier classes ({@code Nullable}, {@code MinusNull}, ...), not the source-level
     * JSpecify annotations the test expects ({@code Nullable}, {@code NonNull}, ...). {@link
     * #INTERNAL_QUALIFIER_TO_PUBLIC_NAME} translates between the two.
     */
    private static final String CONFLICTING_ANNOS_KEY = "type.invalid.conflicting.annos";

    private static final ImmutableMap<String, String> INTERNAL_QUALIFIER_TO_PUBLIC_NAME =
        ImmutableMap.of(
            "Nullable", "Nullable",
            "MinusNull", "NonNull",
            "NullnessUnspecified", "NullnessUnspecified");

    private final TestDiagnostic diagnostic;

    DetailMessageReportedFact(@Nullable Path testDirectory, TestDiagnostic diag) {
      super(
          (testDirectory != null && diag.getFile().startsWith(testDirectory))
              ? testDirectory.relativize(diag.getFile())
              : diag.getFile(),
          diag.getLineNumber());
      this.diagnostic = diag;
    }

    @Override
    protected boolean matches(ExpectedFact expectedFact) {
      if (expectedFact.isNullnessMismatch()) {
        return DEREFERENCE.equals(diagnostic.getMessageKey())
            || CANNOT_CONVERT_KEYS.contains(diagnostic.getMessageKey());
      }
      if (CONFLICTING_ANNOS_KEY.equals(diagnostic.getMessageKey())
          && conflictingAnnotationPublicNames().stream()
              .map(ReportedFact::irrelevantAnnotation)
              .anyMatch(expectedFact::hasFactText)) {
        return true;
      }
      // Falls through even for CONFLICTING_ANNOS_KEY: samples/README.md's own
      // jspecify_conflicting_annotations assertion (distinct from the official corpus's
      // test:irrelevant-annotation:* assertions handled above) is one of ExpectedFact's "optional
      // error assertions", which super.matches() already satisfies for any required-error
      // diagnostic on the same line, including this one.
      return super.matches(expectedFact);
    }

    /**
     * Parses the internal qualifier names out of {@link #CONFLICTING_ANNOS_KEY}'s first argument
     * (an {@code AnnotationMirrorSet}'s {@code toString()}, e.g. {@code "[@Nullable, @MinusNull]"})
     * and translates each to the public JSpecify annotation name that a test's {@code
     * test:irrelevant-annotation:*} comment would name, via {@link
     * #INTERNAL_QUALIFIER_TO_PUBLIC_NAME}.
     */
    private ImmutableList<String> conflictingAnnotationPublicNames() {
      if (!(diagnostic instanceof DetailedTestDiagnostic)) {
        return ImmutableList.of();
      }
      List<String> args = ((DetailedTestDiagnostic) diagnostic).getAdditionalTokens();
      if (args.isEmpty()) {
        return ImmutableList.of();
      }
      String annotationList = args.get(0).replaceAll("[\\[\\]@]", "");
      return Splitter.on(", ")
          .splitToStream(annotationList)
          .map(name -> INTERNAL_QUALIFIER_TO_PUBLIC_NAME.getOrDefault(name, name))
          .collect(toImmutableList());
    }

    @Override
    protected boolean mustBeExpected() {
      return diagnostic.getKind().equals(DiagnosticKind.Error);
    }

    @Override
    protected String getFactText() {
      if (!(diagnostic instanceof DetailedTestDiagnostic)) {
        return toString();
      }
      List<String> args = ((DetailedTestDiagnostic) diagnostic).getAdditionalTokens();
      if (CANNOT_CONVERT_KEYS.contains(diagnostic.getMessageKey())) {
        if (args.size() < 2) {
          // The arguments must end with sourceType and sinkType.
          return toString();
        }
        String sourceType = fixType(args.get(args.size() - 2)); // penultimate
        String sinkType = fixType(args.get(args.size() - 1)); // last
        return cannotConvert(sourceType, sinkType);
      }
      if (IRRELEVANT_ANNOTATION_KEYS.contains(diagnostic.getMessageKey())) {
        if (args.isEmpty()) {
          // arguments must start with the annotation
          return toString();
        }
        return irrelevantAnnotation(
            // Remove the package name (and any enclosing element name); emit just the simple name.
            args.get(0).replaceFirst(".*\\.", ""));
      }
      switch (diagnostic.getMessageKey()) {
        case "sourceType":
          {
            String expressionType = fixType(args.get(0));
            String expression = args.get(1);
            return expressionType(expressionType, expression);
          }
        case "sinkType":
          {
            String sinkType = fixType(args.get(0));
            // Remove the simple name of the class and the dot before the method name.
            String sink = args.get(1).replaceFirst("^[^.]+\\.", "");
            return sinkType(sinkType, sink);
          }
      }
      return toString();
    }

    @Override
    public String toString() {
      return String.format("(%s) %s", diagnostic.getMessageKey(), diagnostic.getMessage());
    }

    /**
     * Rewrite the CF types into JSpecify types.
     *
     * <ul>
     *   <li>Nullness sigils {@code ?}, {@code !}, and {@code *} move from after the type arguments
     *       to before them.
     *   <li>If there is no nullness sigil, use {@code !}. (TODO: What about parametric nullness?)
     *   <li>A captured type variable's arbitrary, non-deterministic numeric identifier (e.g. {@code
     *       capture#237}) is dropped, leaving the bare word {@code capture}, matching the
     *       conformance corpus's own convention (its {@code expression-type} assertions never
     *       include one) -- CF assigns these purely for that compiler run's own diagnostics.
     * </ul>
     */
    private static String fixType(String type) {
      type = CAPTURE_ID.matcher(type).replaceAll("capture");
      Matcher matcher = TYPE.matcher(type);
      if (!matcher.matches()) {
        return type;
      }
      String args = matcher.group("args");
      String suffix = matcher.group("suffix");
      if (args == null && suffix != null) {
        return type;
      }
      StringBuilder newType = new StringBuilder(matcher.group("raw"));
      newType.append(requireNonNullElse(suffix, "!"));
      if (args != null) {
        newType.append(
            COMMA_SPLITTER
                .splitToStream(args)
                .map(DetailMessageReportedFact::fixType)
                .collect(joining(",", "<", ">")));
      }
      return newType.toString();
    }

    private static final Pattern TYPE =
        Pattern.compile("(?<raw>[^<,?!*]+)(?:<(?<args>.+)>)?(?<suffix>[?!*])?");

    private static final Pattern CAPTURE_ID = Pattern.compile("capture#\\d+");

    private static final Splitter COMMA_SPLITTER = Splitter.on(",");
  }
}
