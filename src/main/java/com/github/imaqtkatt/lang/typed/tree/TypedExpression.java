package com.github.imaqtkatt.lang.typed.tree;

import com.github.imaqtkatt.lang.parser.Scope;
import com.github.imaqtkatt.lang.tree.Operation;
import com.github.imaqtkatt.lang.typed.Type;

import java.util.List;

public sealed interface TypedExpression {
    Type type();

    record Variable(Type type, String name, Scope.DeclarationType decType) implements TypedExpression {
    }

    record Int(Type type, Integer i) implements TypedExpression {
    }

    record Bool(Type type, Boolean b) implements TypedExpression {
    }

    record Binary(Type type, TypedExpression left, Operation op, TypedExpression right) implements TypedExpression {
    }

    record Call(Type type, TypedExpression callee, List<TypedExpression> arguments) implements TypedExpression {
    }

    record Let(Type type, String bind, TypedExpression value, TypedExpression body) implements TypedExpression {
    }

    record Seq(Type type, TypedExpression left, TypedExpression right) implements TypedExpression {
    }

    record Mutable(Type type, TypedExpression base) implements TypedExpression {
    }

    record Deref(Type type, TypedExpression mutable) implements TypedExpression {
    }

    record If(Type type, TypedExpression condition, TypedExpression then,
              TypedExpression otherwise) implements TypedExpression {
    }

    record Lambda(Type type, List<String> parameters, TypedExpression body) implements TypedExpression {
    }
}
