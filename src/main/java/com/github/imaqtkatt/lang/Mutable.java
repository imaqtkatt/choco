package com.github.imaqtkatt.lang;

import java.util.concurrent.atomic.AtomicReference;

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
