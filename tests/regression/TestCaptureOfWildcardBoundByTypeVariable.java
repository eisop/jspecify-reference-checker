import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A capture whose upper bound is itself a type variable is only nullable relative to that type
 * variable, not definitely nullable -- unlike the capture of {@code ? extends SomeNullableType},
 * which {@code isCaptureOfDefinitelyNullableExtendsWildcard} exists to project as UNION_NULL.
 */
@NullMarked
abstract class TestCaptureOfWildcardBoundByTypeVariable {
  interface Sup<T extends @Nullable Object> {
    T get();
  }

  // Should NOT produce a nullness mismatch: the capture of `? extends V` is a subtype of V, so
  // returning it where V is expected is fine, regardless of what V is instantiated with.
  abstract <V extends @Nullable Object> V viaWildcard(Sup<? extends V> s);

  <V extends @Nullable Object> V x(Sup<? extends V> s) {
    return s.get();
  }
}
