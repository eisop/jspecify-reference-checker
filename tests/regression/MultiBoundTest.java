import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class MultiBoundTest {
  interface A {}
  interface B {}

  // T extends @Nullable A & B: B is non-null, so T must be non-null.
  // isNullExclusiveUnderEveryParameterization(T) should be TRUE.
  // Returning T as Object! (non-null) should be FINE - no error.
  <T extends @Nullable A & B> Object nullableFirstNonNullSecond(T t) {
    return t; // should NOT be an error
  }

  // T extends A & @Nullable B: A is non-null, so T must be non-null.
  // Returning T as Object! should be FINE - no error.
  <T extends A & @Nullable B> Object nonNullFirstNullableSecond(T t) {
    return t; // should NOT be an error
  }

  // T extends @Nullable A & @Nullable B: both nullable - T can be null.
  // Returning T as Object! should be an error.
  <T extends @Nullable A & @Nullable B> Object bothNullable(T t) {
    // :: error: jspecify_nullness_mismatch
    return t;
  }
  
  // T extends A & B: both non-null (default in NullMarked).
  // Returning T as Object! should be FINE - no error.
  <T extends A & B> Object bothNonNull(T t) {
    return t; // should NOT be an error
  }
}
