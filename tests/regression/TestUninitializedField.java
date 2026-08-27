import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class TestUninitializedField {
  // Assigned in a constructor through a bare identifier.
  Object assignedBare;

  // Assigned in a constructor through an explicit `this.`.
  Object assignedViaThis;

  // Assigned in an instance initializer block, which javac copies into every constructor.
  Object assignedInInitializerBlock;

  // Nullable, so the implicit null the JVM writes is fine.
  @Nullable Object nullableNeverAssigned;

  // :: error: field.uninitialized
  Object neverAssigned;

  // Assigned only on some *other* instance, which does not initialize this one.
  // :: error: field.uninitialized
  Object assignedOnOtherInstance;

  {
    assignedInInitializerBlock = new Object();
  }

  TestUninitializedField(TestUninitializedField other) {
    assignedBare = new Object();
    this.assignedViaThis = new Object();
    other.assignedOnOtherInstance = new Object();
  }
}
