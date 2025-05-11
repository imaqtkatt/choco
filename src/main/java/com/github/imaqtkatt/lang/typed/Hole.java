package com.github.imaqtkatt.lang.typed;

public sealed interface Hole {
    record Bound(Type type) implements Hole {}

    record Unbound(Integer id, Integer level) implements Hole {}
}
