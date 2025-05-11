package com.github.imaqtkatt.lang.typed.tree;

import com.github.imaqtkatt.lang.typed.Type;

import java.util.List;

public sealed interface Node {
    Type type();

    record FunDefinition(Type.Fun type, List<Type> paramsTypes, String name, List<String> params, TypedExpression body) implements Node {
    }

    record ValDefinition(Type type, String name, TypedExpression value) implements Node {
    }

    record Import(String name) implements Node {
        @Override
        public Type type() {
            return null;
        }
    }
}
