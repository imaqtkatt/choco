package com.github.imaqtkatt.lang.typed;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public record Scheme(List<Integer> binds, Type type) {
    public Type instantiate() {
        var table = new HashMap<Integer, Type>();
        binds.forEach((bind) -> table.put(bind, HoleGen.newHole()));
        return replace(table, type);
    }

    private static Type replace(Map<Integer, Type> table, Type type) {
        return switch (type) {
            case Type.Fun(List<Type> params, Type ret) -> new Type.Fun(
                    params.stream().map((param) -> replace(table, param)).toList(),
                    replace(table, ret)
            );

            case Type.Hole(HoleRef ref) -> switch (ref.get()) {
                case Hole.Bound(Type bound) -> replace(table, bound);
                case Hole.Unbound(Integer id, Integer ignored) -> table.getOrDefault(id, type);
            };

            case Type.Mutable(Type inner) -> new Type.Mutable(replace(table, inner));

            case Type.TString tString -> type;
            case Type.TVoid tVoid -> type;
            case Type.Bool bool -> type;
            case Type.Int anInt -> type;
        };
    }

    public static Scheme ofType(Type type) {
        return new Scheme(List.of(), type);
    }

    public static Scheme generalized(Type type) {
        List<Integer> tvs = new LinkedList<>();
        typeVariables(tvs, type);
        return new Scheme(tvs, type);
    }

    private static void typeVariables(List<Integer> list, Type type) {
        switch (type) {
            case Type.Hole(HoleRef ref) -> {
                switch (ref.get()) {
                    case Hole.Bound(Type type1) -> typeVariables(list, type1);
                    case Hole.Unbound(Integer id, Integer level) -> {
                        if (level > HoleGen.currentLevel()) {
                            list.addLast(id);
                        }
                    }
                }
            }

            case Type.Fun fun -> {
                fun.params().forEach((p) -> typeVariables(list, p));
                typeVariables(list, fun.ret());
            }

            case Type.Mutable mutable -> {
                typeVariables(list, mutable.inner());
            }

            case Type.Int anInt -> {
            }
            case Type.TString tString -> {
            }
            case Type.Bool bool -> {
            }
            case Type.TVoid tVoid -> {
            }
        }
    }
}
