package com.github.imaqtkatt.lang.tree;

import java.util.List;

public sealed interface Node {
    record FunDefinition(String name, List<String> params, Expression body) implements Node {}

    record ValDefinition(String name, Expression value) implements Node {}

    record Import(String name) implements Node {}
}
