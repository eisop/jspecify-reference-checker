import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

@NullMarked
abstract class TestCaptureInferenceStrict {
  // Null-unmarked so bare `<T>` defaults to unspecified nullness without explicit annotations.
  @NullUnmarked
  interface UnspecBounded<T> {
    T get();
  }

  interface NullableBounded<T extends @Nullable Object> {
    T get();
  }

  interface Lib {}

  abstract <T extends @Nullable Object> @Nullable T unionNull(T input);

  // Should not produce type.arguments.not.inferred in strict mode:
  @Nullable Object x6(UnspecBounded<? extends Lib> x) {
    return unionNull(x.get());
  }

  // Null-unmarked so explicit wildcard bound `? extends Lib` defaults to unspecified nullness.
  @NullUnmarked
  @Nullable Object x7(UnspecBounded<? extends Lib> x) {
    return unionNull(x.get());
  }

  @Nullable Object x9(NullableBounded<? extends Lib> x) {
    return unionNull(x.get());
  }
}
