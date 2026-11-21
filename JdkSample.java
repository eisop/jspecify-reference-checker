import org.jspecify.annotations.NullMarked;

// `Provider.put` returns `@Nullable` only in the JSpecify annotated JDK, so the demo reports the
// dereference only if it uses that JDK rather than the Checker Framework's own.
@NullMarked
class JdkSample {
  int hash(java.security.Provider provider) {
    return provider.put("a", "b").hashCode();
  }
}
