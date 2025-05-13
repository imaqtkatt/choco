package choco.lang;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A generic container that wraps a mutable reference to an object.
 * <p>
 * This class provides a way to simulate mutable variables in Java,
 * particularly useful when working with functional-style programming
 * or closures where mutation is otherwise restricted.
 * <p>
 * The wrapped value is stored in an {@link AtomicReference},
 * allowing for safe concurrent updates and access.
 *
 * @param <O> the type of the object being wrapped
 */
public final class Mutable<O> {

    private final AtomicReference<O> inner;

    public static <O> Mutable<O> of(O object) {
        return new Mutable<>(object);
    }

    public static <O> void set(Mutable<O> mutable, O object) {
        mutable.inner.set(object);
    }

    public static <O> O deref(Mutable<O> mutable) {
        return mutable.inner.get();
    }

    public Mutable(O object) {
        inner = new AtomicReference<>(object);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
