package com.github.imaqtkatt.lang.typed;

import java.util.List;

public final class Unify {
    public static boolean unify(Type a, Type b, boolean unifyVoid) {
        var pair = new Pair(a, b);
        // System.out.println("pair = " + pair);
        return switch (pair) {
            case Pair(Type.Hole(HoleRef a_), Type b_) -> unifyHole(a_, b_, unifyVoid, false);
            case Pair(Type a_, Type.Hole(HoleRef b_)) -> unifyHole(b_, a_, unifyVoid, true);

            case Pair(Type.Mutable(Type a_), Type.Mutable(Type b_)) -> unify(a_, b_, unifyVoid);

            case Pair(Type.Fun a_, Type.Fun b_) -> {
                if (a_.params().size() != b_.params().size()) {
//                    yield false;
                    throw new RuntimeException("Arity error between '%s' and '%s'".formatted(a, b));
                }

                boolean retVal = unify(a_.ret(), b_.ret(), true);

                for (int i = 0; i < a_.params().size(); i++) {
                    retVal |= unify(a_.params().get(i), b_.params().get(i), false);
                }
                //retVal |= unify(a_.ret(), b_.ret(), true);

                yield retVal;
            }

            case Pair(Type.Bool(), Type.Bool()), Pair(Type.Int(), Type.Int()), Pair(Type.TString(), Type.TString()) ->
                    true;
            case Pair(Type.TVoid(), Type.TVoid()) -> unifyVoid;

            default -> {
                throw new RuntimeException("Type mismatch between '%s' and '%s'".formatted(a, b));
            }
        };
    }

    private static boolean unifyHole(HoleRef hole, Type type, boolean unifyVoid, boolean swap) {
        return switch (hole.get()) {
            case Hole.Bound bound -> swap ? unify(type, bound.type(), unifyVoid) : unify(bound.type(), type, unifyVoid);

            case Hole.Unbound ignored -> {
                if (occurs(hole, type)) {
                    yield false;
                } else {
                    hole.fill(type);
                    yield true;
                }
            }
        };
    }

    public static boolean occurs(HoleRef hole, Type t) {
        return switch (t) {
            case Type.Hole inner -> hole == inner.ref();

            case Type.Fun(List<Type> params, Type ret) -> params.stream().anyMatch((c) -> occurs(hole, c)) ||
                    occurs(hole, ret);

            case Type.Mutable(Type inner) -> occurs(hole, inner);

            case Type.Bool(), Type.Int(), Type.TString(), Type.TVoid() -> false;
        };
    }

    private record Pair(Type a, Type b) {
    }
}
