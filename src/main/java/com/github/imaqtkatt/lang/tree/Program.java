package com.github.imaqtkatt.lang.tree;

import java.util.List;

public record Program(String packageName, List<Node> definitions) {
}
