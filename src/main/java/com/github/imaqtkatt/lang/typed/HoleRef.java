package com.github.imaqtkatt.lang.typed;

import choco.lang.Mutable;

public final class HoleRef {
    final Mutable<Hole> inner;

    public HoleRef(Hole hole) {
        inner = Mutable.of(hole);
    }

    public Hole get() {
        return Mutable.deref(inner);
    }

    public void fill(Type type) {
        Mutable.set(inner, new Hole.Bound(type));
    }
}
