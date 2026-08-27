import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Substituting an inferred method type argument into a use with an unspecified nullness operator,
 * for the shapes in the samples' ComplexParametric.
 */
@NullMarked
class TestInferredUnspecTypeArgument {
  interface Foo {}

  interface Lib<T extends @Nullable Object> {}

  interface SuperSuper<T extends @Nullable Object> {
    Lib<T> t();

    Lib<@Nullable T> tUnionNull();

    // Null-unmarked so the bare type argument `T`/`U` defaults to unspecified (TYPE_VARIABLE_USE's
    // default outside null-marked scope), instead of writing `@NullnessUnspecified T`/`U`.
    @NullUnmarked
    void checkTUnspec(Lib<T> lib);

    @NullUnmarked
    <U extends @Nullable Object> void checkUnspecNull(Lib<U> lib);
  }

  /** The intersection's non-null {@code Foo} bound makes {@code T} null-exclusive. */
  interface UnionNullNever<T extends @Nullable Object & Foo> extends SuperSuper<T> {
    default void x() {
      checkUnspecNull(t());
      checkTUnspec(t());
      this.<T>checkUnspecNull(tUnionNull());
    }
  }

  /** Both bounds are nullable, so {@code T} definitely may be null. */
  interface UnionNullUnionNull<T extends @Nullable Object & @Nullable Foo> extends SuperSuper<T> {
    default void x() {
      // The inferred `U` is a definitely nullable `T`, so the parameter is `Lib<@Nullable T>`,
      // which the `Lib<T>` argument does not match even in the most convenient world.
      // :: error: (argument.type.incompatible)
      checkUnspecNull(t());
      // A written type argument is honored as written, leaving the parameter `Lib<T*>`.
      this.<T>checkUnspecNull(tUnionNull());
      // Likewise for a class type parameter, which is never inferred.
      checkTUnspec(t());
    }
  }
}
