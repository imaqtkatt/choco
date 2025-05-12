package com.github.imaqtkatt.lang.parser;

public enum Precedence {
    Start(0),

    Seq(1),

    Set(2),

    Compare(3),
    Logic(4),
    Sum(5),
    Product(6),

    End(7);

    final int value;

    Precedence(int i) {
        value = i;
    }

    public Precedence left() {
        return switch (this) {
            case Start -> Seq;
            case Seq -> Set;
            case Set -> Compare;
            case Compare -> Logic;
            case Logic -> Sum;
            case Sum -> Product;
            case Product -> End;
            case End -> throw new RuntimeException();
        };
    }
}
