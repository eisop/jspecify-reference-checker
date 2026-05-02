// Test that mimics jspecify's MultiBoundTypeVariableToObject.java samples
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class MultiBoundSampleLike {
  // x0: T extends Object & Lib -> both non-null -> no error
  <T extends Object & Lib> Object x0(T x) {
    return x;
  }

  // x2: T extends Object & @Nullable Lib -> Object is non-null -> no error
  <T extends Object & @Nullable Lib> Object x2(T x) {
    return x;
  }

  // x6: T extends @Nullable Object & Lib -> Lib is non-null -> no error
  <T extends @Nullable Object & Lib> Object x6(T x) {
    return x;
  }

  // x8: T extends @Nullable Object & @Nullable Lib -> both nullable -> error
  <T extends @Nullable Object & @Nullable Lib> Object x8(T x) {
    // :: error: jspecify_nullness_mismatch
    return x;
  }

  interface Lib {}
}
