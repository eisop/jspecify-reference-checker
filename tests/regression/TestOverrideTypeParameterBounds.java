import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tests for generic method overrides where a type-parameter bound differs (narrower or wider) from
 * the overridden method.
 *
 * <p>The overridden signature is adapted to the type parameters of the overriding method (JLS
 * 8.4.4) before its parameter and return types are compared, so every occurrence of a type
 * parameter, however nested, is the same type variable on both sides. A difference between the
 * bounds is therefore compared only by the declaration-level check ({@code
 * isTypeParameterBoundOverrideValid}): the overriding type-parameter upper bound must not be
 * narrower than the overridden bound. Narrowing is reported once, as {@code
 * override.typaram.invalid}, wherever the type parameter occurs; widening is permitted. An
 * occurrence is still reported if it is itself unrelated or requalified.
 */
@NullMarked
class TestOverrideTypeParameterBounds {
  interface Sequence<E extends @Nullable Object> {}

  abstract static class Sup {
    // ---- Narrowing (upper bound goes from @Nullable to the implicit @NonNull). ----

    // Bare parameter: the occurrence is the same type variable on both sides, so only the
    // declaration-level rule reports the narrowed bound.
    abstract <T extends @Nullable Object> void bareParam(T t);

    // Nested in a parameterized type argument: only the declaration-level rule reports it.
    abstract <U extends @Nullable Object> Sequence<U> nestedOnly(Sequence<U> in);

    // The same type parameter occurs both bare and nested: the declaration-level rule reports
    // once per type parameter, not once per occurrence.
    abstract <V extends @Nullable Object> void mixed(V v, Sequence<V> s);

    // Bare RETURN type: only the declaration-level rule reports the narrowed bound.
    abstract <W extends @Nullable Object> W bareReturn();

    // Bare parameter requalified with its own explicit @Nullable, regardless of the type
    // parameter's bound: the explicit annotation makes the occurrence itself trivially
    // compatible on both sides, so only the declaration-level rule has anything to catch.
    abstract <X extends @Nullable Object> void qualifiedBareParam(@Nullable X x);

    // Same, for a return type.
    abstract <Y extends @Nullable Object> @Nullable Y qualifiedBareReturn();

    // Array of a bare type variable: reported once, like the bare case.
    abstract <Z extends @Nullable Object> void arrayParam(Z[] z);

    // Varargs desugars to an array parameter type, so this is reported the same way.
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

    // Bare RETURN type, WIDENED: sound -- both the occurrence and the declaration-level rule
    // accept widening the upper bound. No diagnostic at all.
    abstract <E1 extends Object> E1 bareReturnWiden();

    // Nested, widened: same soundness as bareReturnWiden. No diagnostic.
    abstract <F1 extends Object> Sequence<F1> nestedReturnWiden();

    // Array of a bare type variable, widened: the element type is the same type variable on both
    // sides, so this is accepted like the bare case. No diagnostic.
    abstract <G1 extends Object> G1[] arrayReturnWiden();

    // Widening a parameter's bound: accepted, like bareReturnWiden. No diagnostic.
    abstract <H1 extends Object> void bareParamWiden(H1 h);

    // Nested parameter, widened: accepted. No diagnostic.
    abstract <I1 extends Object> void nestedParamWiden(Sequence<I1> in);
  }

  abstract static class Sub extends Sup {
    @Override
    // :: error: (override.typaram.invalid)
    abstract <T> void bareParam(T t);

    @Override
    // :: error: (override.typaram.invalid)
    abstract <U> Sequence<U> nestedOnly(Sequence<U> in);

    @Override
    // :: error: (override.typaram.invalid)
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
    // :: error: (override.typaram.invalid)
    abstract <Z> void arrayParam(Z[] z);

    @Override
    // :: error: (override.typaram.invalid)
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
    abstract <G1 extends @Nullable Object> G1[] arrayReturnWiden();

    @Override
    abstract <H1 extends @Nullable Object> void bareParamWiden(H1 h);

    @Override
    abstract <I1 extends @Nullable Object> void nestedParamWiden(Sequence<I1> in);
  }
}
