package com.github.imaqtkatt.lang.parser;

public enum Precedence {
    Start(0),

    Seq(1),

    Set(2),

    Logic(3),
    Sum(4),
    Product(5),

    End(6);

    final int value;

    Precedence(int i) {
        value = i;
    }

    public Precedence left() {
        return switch (this) {
            case Start -> Seq;
            case Logic -> Sum;
            case Sum -> Product;
            case Product -> End;
            case Seq -> Set;
            case Set -> Logic;
            case End -> throw new RuntimeException();
        };
    }
}
