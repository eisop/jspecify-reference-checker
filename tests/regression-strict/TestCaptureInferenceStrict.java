import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

@NullMarked
abstract class TestCaptureInferenceStrict {
  // Null-unmarked so that the bare `<T>` bound defaults to unspecified (per the spec rule for the
  // bound of an Object-bounded type parameter outside a null-marked scope), without writing the
  // fork-only `@NullnessUnspecified` annotation.
  @NullUnmarked
  interface UnspecBounded<T> {
    T get();
  }

  interface NullableBounded<T extends @Nullable Object> {
    T get();
  }

  interface Lib {}

  abstract <T extends @Nullable Object> @Nullable T unionNull(T input);

  // Should NOT produce type.arguments.not.inferred in strict mode:
  @Nullable Object x6(UnspecBounded<? extends Lib> x) {
    return unionNull(x.get());
  }

  // Null-unmarked so the explicit wildcard upper bound `? extends Lib` also defaults to
  // unspecified (falls through to OTHERWISE, which is unspecified outside null-marked scope).
  @NullUnmarked
  @Nullable Object x7(UnspecBounded<? extends Lib> x) {
    return unionNull(x.get());
  }

  @Nullable Object x9(NullableBounded<? extends Lib> x) {
    return unionNull(x.get());
  }
}
