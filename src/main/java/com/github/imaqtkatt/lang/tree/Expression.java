package com.github.imaqtkatt.lang.tree;

import com.github.imaqtkatt.lang.parser.Scope;

import java.util.List;

public sealed interface Expression {
    record Variable(String name, Scope.DeclarationType decType) implements Expression {
    }

    /**
     * An integer constant.
     * <pre>
     * {@code
     * let ten = 10
     * }
     * </pre>
     *
     * @param i
     */
    record Int(Integer i) implements Expression {
    }

    /**
     * A boolean value.
     * <pre>
     * {@code
     * let t = true
     * let f = false
     * }
     * </pre>
     *
     * @param b
     */
    record Bool(Boolean b) implements Expression {
    }

    /**
     * A binary expression.
     * <pre>
     * {@code
     * 1 <op> 2
     * }
     * </pre>
     *
     * @param left
     * @param op
     * @param right
     */
    record Binary(Expression left, Operation op, Expression right) implements Expression {
    }

    /**
     * A call expression.
     * <pre>
     * {@code
     * add(1, 2)
     * }
     * </pre>
     *
     * @param callee
     * @param arguments
     */
    record Call(Expression callee, List<Expression> arguments) implements Expression {
    }

    /**
     * A binding for a variable.
     * <pre>
     * {@code
     * let bind = 10 in
     * <body>
     * }
     * </pre>
     *
     * @param bind
     * @param value
     * @param body
     */
    record Let(String bind, Expression value, Expression body) implements Expression {
    }

    /**
     * Sequence to an expression. The left expression must be of type void.
     * <pre>
     * {@code
     * fun prints() =
     *   println("ok");
     *   true
     * }
     * </pre>
     *
     * @param left
     * @param right
     */
    record Seq(Expression left, Expression right) implements Expression {
    }

    /**
     * Creates a new mutable instance.
     * <pre>
     * {@code
     * let counter = mutable 0 in
     * !counter
     * }
     * </pre>
     *
     * @param base
     */
    record Mutable(Expression base) implements Expression {
    }

    /**
     * Dereferences a mutable instance.
     * <pre>
     * {@code
     * let x = mutable 0 in
     * deref x
     * </pre>
     *
     * @param mutable
     */
    record Deref(Expression mutable) implements Expression {
    }

    /**
     * Boolean branch expression.
     * <pre>
     * {@code
     * if true then 1 else 0
     * }
     * </pre>
     * @param condition
     * @param then
     * @param otherwise
     */
    record If(Expression condition, Expression then, Expression otherwise) implements Expression {
    }

    /**
     * An anonymous function.
     * <pre>
     * {@code
     * ->(x, y) { x + y }
     * }
     * </pre>
     * @param parameters
     * @param body
     */
    record Lambda(List<String> parameters, Expression body) implements Expression {
    }
}
