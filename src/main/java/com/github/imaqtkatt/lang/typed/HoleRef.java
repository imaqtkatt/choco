package com.github.imaqtkatt.lang.typed;

import com.github.imaqtkatt.lang.Mutable;

public final class HoleRef {
    final Mutable<Hole> inner;

    public HoleRef(Hole hole) {
        inner = new Mutable<>(hole);
    }

    public Hole get() {
        return Mutable.deref(inner);
    }

    public void fill(Type type) {
        Mutable.set(inner, new Hole.Bound(type));
    }
}
