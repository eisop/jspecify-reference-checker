// Test for captured wildcard return type issue
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class CaptureTest {
  // This PASSES already (from BoundedTypeVariableReturn)
  // F_cap from unbounded ? with @Nullable Foo bound
  Foo passing(NullableFooSupplier<?> supplier) {
    // :: error: return.type.incompatible
    return supplier.get();
  }

  // This should also fail but DOESN'T
  // T_cap from ? extends @Nullable Lib with NullableBounded<T extends @Nullable Object>
  Object failing(NullableBounded<? extends Lib> x) {
    // :: error: return.type.incompatible
    return x.get();
  }

  interface NullableFooSupplier<F extends @Nullable Foo> {
    F get();
  }

  interface NullableBounded<T extends @Nullable Object> {
    T get();
  }

  interface Foo {}
  interface Lib {}
}
