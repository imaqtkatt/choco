package com.github.imaqtkatt.lang.typed.infer;

import com.github.imaqtkatt.lang.typed.Environment;
import com.github.imaqtkatt.lang.typed.tree.Program;

public final class InferProgram {
    public static Program inferProgram(com.github.imaqtkatt.lang.tree.Program program) {
        Environment environment = new Environment();
        var definitions = program.definitions()
                .stream()
                .map((definition) -> {
                    var result = InferNode.infer(environment, definition);
                    System.out.printf("%s |- %s%n", result, result.type());
                    return result;
                })
                .toList();
        return new Program(program.packageName(), definitions);
    }
}
