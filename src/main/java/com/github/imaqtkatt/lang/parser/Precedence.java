package com.github.imaqtkatt.lang.parser;

public enum Precedence {
    Start(0),

    Seq(1),

    Set(2),

    Or(3),
    And(4),

    Compare(5),
    Logic(6),
    Sum(7),
    Product(8),

    End(9);

    final int value;

    Precedence(int i) {
        value = i;
    }

    public Precedence left() {
        return switch (this) {
            case Start -> Seq;
            case Seq -> Set;
            case Set -> Or;
            case Or -> And;
            case And -> Compare;
            case Compare -> Logic;
            case Logic -> Sum;
            case Sum -> Product;
            case Product -> End;
            case End -> throw new RuntimeException();
        };
    }
}
