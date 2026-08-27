import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A generic method whose own type parameter's bound differs -- narrower (more definitely non-null)
 * or wider (more definitely nullable) -- from the corresponding type parameter's bound on the
 * method it overrides.
 *
 * <p>checker-framework's {@code BaseTypeVisitor.OverrideChecker} enforces this with two independent
 * rules that this checker does not try to deduplicate against each other (matching
 * checker-framework's own {@code checker/tests/nullness/OverrideTypeParamBound.java}, which accepts
 * the same overlap for CF's built-in Nullness Checker):
 *
 * <ul>
 *   <li>{@code isTypeParameterBoundOverrideValid} (checker-framework's sound-by-default
 *       declaration-level check, unmodified by this checker): compares the two type parameters'
 *       declared bounds directly, regardless of whether, or where, the type parameter is used.
 *       Since {@link #atypeFactory}'s upper bound alone determines a bare type variable's nullness
 *       in this checker (a declared <em>lower</em> bound is not a feature JSpecify uses), this
 *       reduces to: the overriding upper bound must not be <em>narrower</em> (more definitely
 *       non-null) than the overridden one. <b>Widening is accepted</b> -- see checker-framework's
 *       own {@code SubWidenUpperReturn}/{@code SubImplicitBound} for why that is sound regardless
 *       of position. A mismatch reports {@code override.typaram.invalid}.
 *   <li>{@code NullSpecOverrideChecker#isParameterOverrideValid} (this checker's own addition,
 *       unrelated to the rule above): requires full parameter <em>invariance</em>, not just
 *       checker-framework's default contravariance. For a <em>bare</em> parameter occurrence of a
 *       type parameter whose own bound differs at all -- narrower or wider -- this independently
 *       fails and reports {@code override.param.invalid}, regardless of what the declaration-level
 *       rule above decides for that same mismatch.
 * </ul>
 *
 * <p>Two more checker-framework mechanics shape the table below:
 *
 * <ul>
 *   <li>A bare <em>return</em> occurrence is covariant, not invariant, and checker-framework's
 *       {@code testTypevarContainment} fallback (which {@code isReturnOverrideValid} and {@code
 *       isParameterOverrideValid} both fall back to when the plain subtype check fails) now uses
 *       the same containment direction as the declaration-level rule. So a bare return occurrence
 *       never independently reports a widened upper bound (sound, matching the declaration-level
 *       rule) and never independently reports a narrowed one either (ordinary covariance already
 *       accepts a narrower return type) -- only {@code override.typaram.invalid} ever catches
 *       narrowing there, and nothing catches widening.
 *   <li>{@code testTypevarContainment} only ever applies to a type variable occurring bare --
 *       {@code inner.getKind() == TypeKind.TYPEVAR}, checked before anything else -- so it never
 *       rescues an <em>array</em> (or varargs, which desugars to one) occurrence. An array
 *       occurrence's ordinary subtype check therefore independently fails for <em>any</em> bound
 *       difference, in either direction, with no fallback to save it; only {@code
 *       override.typaram.invalid}'s widening tolerance is direction-sensitive, so a narrowed array
 *       gets both diagnostics but a widened one gets only the array-specific one.
 *   <li>{@code NullSpecAnnotatedTypeFactory.NullSpecEqualityComparer#areNestedTypesEqual}
 *       deliberately never recurses into a type variable's bounds, so a type parameter used only
 *       <em>nested</em> inside a parameterized type argument (e.g. {@code Sequence<T>}) is
 *       structurally invisible to every occurrence-level check, in both parameter and return
 *       position -- only {@code override.typaram.invalid} can ever report it, and (per the first
 *       bullet) only for narrowing.
 * </ul>
 */
@NullMarked
class TestOverrideTypeParameterBounds {
  interface Sequence<E extends @Nullable Object> {}

  abstract static class Sup {
    // ---- Narrowing (upper bound goes from @Nullable to the implicit @NonNull). ----

    // Bare parameter: JSpecify's own invariance rule independently rejects this (any bound
    // change at a bare parameter occurrence does), in addition to the declaration-level rule.
    abstract <T extends @Nullable Object> void bareParam(T t);

    // Nested in a parameterized type argument: invisible to every occurrence-level check, so
    // only the declaration-level rule reports it.
    abstract <U extends @Nullable Object> Sequence<U> nestedOnly(Sequence<U> in);

    // The same type parameter occurs both bare and nested: still the same two diagnostics as
    // bareParam -- the declaration-level rule reports once per type parameter, not once per
    // occurrence, and the bare occurrence still trips JSpecify's own invariance rule.
    abstract <V extends @Nullable Object> void mixed(V v, Sequence<V> s);

    // Bare RETURN type: covariance already accepts a narrower return type, so nothing but the
    // declaration-level rule catches this.
    abstract <W extends @Nullable Object> W bareReturn();

    // Bare parameter requalified with its own explicit @Nullable, regardless of the type
    // parameter's bound: the explicit annotation makes the occurrence itself trivially
    // compatible on both sides, so only the declaration-level rule has anything to catch.
    abstract <X extends @Nullable Object> void qualifiedBareParam(@Nullable X x);

    // Same, for a return type.
    abstract <Y extends @Nullable Object> @Nullable Y qualifiedBareReturn();

    // Array of a bare type variable: arrays are never rescued by testTypevarContainment's
    // fallback, so the ordinary parameter check independently fails here too.
    abstract <Z extends @Nullable Object> void arrayParam(Z[] z);

    // Varargs desugars to an array parameter type, so this fails the same way.
    abstract <A1 extends @Nullable Object> void varargsParam(A1... a);

    // Not used in any parameter or return type at all: nothing else could possibly catch this,
    // so the declaration-level rule is the only path that ever reports it.
    abstract <B1 extends @Nullable Object> void unused();

    // Bare return plus a merely-nested parameter occurrence: neither is independently caught
    // (covariance accepts the narrower return; the nested parameter is invisible), so only the
    // declaration-level rule reports it.
    abstract <C1 extends @Nullable Object> C1 bareReturnNestedParam(Sequence<C1> in);

    // The return check fails here, but for a reason of its own (@Nullable D1 is not Object), not
    // because of the narrowed bound -- so both diagnostics are genuine and unrelated.
    abstract <D1 extends @Nullable Object> Object unrelatedReturnFailure();

    // ---- Widening (upper bound goes from the implicit @NonNull to @Nullable). ----

    // Bare RETURN type, WIDENED: sound -- checker-framework's own testTypevarContainment fallback
    // and the declaration-level rule both now accept widening a bare return occurrence's upper
    // bound. No diagnostic at all.
    abstract <E1 extends Object> E1 bareReturnWiden();

    // Nested, widened: same soundness as bareReturnWiden, and invisible to any occurrence-level
    // check regardless. No diagnostic.
    abstract <F1 extends Object> Sequence<F1> nestedReturnWiden();

    // Array of a bare type variable, widened: unlike the bare case, arrays get no containment
    // fallback, so the ordinary return check independently rejects this even though widening is
    // otherwise sound -- the declaration-level rule does not (it accepts widening), so this is
    // the one array case with only a single diagnostic.
    abstract <G1 extends Object> G1[] arrayReturnWiden();

    // Widening a parameter's bound: the declaration-level rule accepts this (same reasoning as
    // bareReturnWiden), but JSpecify's own invariance rule rejects any bare-parameter bound
    // change regardless of direction, so only override.param.invalid fires.
    abstract <H1 extends Object> void bareParamWiden(H1 h);

    // Nested parameter, widened: sound under the declaration-level rule, and JSpecify's own
    // invariance rule does not see a nested occurrence either (the same equality comparer that
    // hides it from checker-framework's default check hides it here too). No diagnostic.
    abstract <I1 extends Object> void nestedParamWiden(Sequence<I1> in);
  }

  abstract static class Sub extends Sup {
    @Override
    // :: error: (override.param.invalid) :: error: (override.typaram.invalid)
    abstract <T> void bareParam(T t);

    @Override
    // :: error: (override.typaram.invalid)
    abstract <U> Sequence<U> nestedOnly(Sequence<U> in);

    @Override
    // :: error: (override.param.invalid) :: error: (override.typaram.invalid)
    abstract <V> void mixed(V v, Sequence<V> s);

    @Override
    // :: error: (override.typaram.invalid)
    abstract <W> W bareReturn();

    @Override
    // :: error: (override.typaram.invalid)
    abstract <X> void qualifiedBareParam(@Nullable X x);

    @Override
    // :: error: (override.typaram.invalid)
    abstract <Y> @Nullable Y qualifiedBareReturn();

    @Override
    // :: error: (override.param.invalid) :: error: (override.typaram.invalid)
    abstract <Z> void arrayParam(Z[] z);

    @Override
    // :: error: (override.param.invalid) :: error: (override.typaram.invalid)
    abstract <A1> void varargsParam(A1... a);

    @Override
    // :: error: (override.typaram.invalid)
    abstract <B1> void unused();

    @Override
    // :: error: (override.typaram.invalid)
    abstract <C1> C1 bareReturnNestedParam(Sequence<C1> in);

    @Override
    // :: error: (override.return.invalid) :: error: (override.typaram.invalid)
    abstract <D1> @Nullable D1 unrelatedReturnFailure();

    @Override
    abstract <E1 extends @Nullable Object> E1 bareReturnWiden();

    @Override
    abstract <F1 extends @Nullable Object> Sequence<F1> nestedReturnWiden();

    @Override
    // :: error: (override.return.invalid)
    abstract <G1 extends @Nullable Object> G1[] arrayReturnWiden();

    @Override
    // :: error: (override.param.invalid)
    abstract <H1 extends @Nullable Object> void bareParamWiden(H1 h);

    @Override
    abstract <I1 extends @Nullable Object> void nestedParamWiden(Sequence<I1> in);
  }
}
