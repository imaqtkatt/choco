package com.github.imaqtkatt.lang.typed.infer;

import com.github.imaqtkatt.lang.typed.Environment;
import com.github.imaqtkatt.lang.typed.HoleGen;
import com.github.imaqtkatt.lang.typed.Scheme;
import com.github.imaqtkatt.lang.typed.Type;
import com.github.imaqtkatt.lang.typed.tree.Node;

import java.util.ArrayList;

public final class InferNode {
    public static Node infer(Environment environment, com.github.imaqtkatt.lang.tree.Node node) {
        return switch (node) {
            case com.github.imaqtkatt.lang.tree.Node.FunDefinition funDefinition ->
                    inferFunNode(environment, funDefinition);
            case com.github.imaqtkatt.lang.tree.Node.Import anImport -> new Node.Import(anImport.name());
            case com.github.imaqtkatt.lang.tree.Node.ValDefinition valDefinition ->
                    inferValNode(environment, valDefinition);
        };
    }

    private static Node inferFunNode(Environment environment, com.github.imaqtkatt.lang.tree.Node.FunDefinition fun) {
//        Environment newEnvironment = environment.clone();
        var parameters = new ArrayList<Type>();

        HoleGen.enterLevel();
        fun.params().forEach((param) -> {
            var hole = HoleGen.newHole();
            environment.insert(param, Scheme.ofType(hole));
            parameters.add(hole);
        });
        HoleGen.leaveLevel();

        var inferredBody = InferExpression.infer(environment, fun.body());
        var funType = new Type.Fun(parameters, inferredBody.type());

        fun.params().forEach(environment::remove);

        environment.declare(fun.name(), Scheme.generalized(funType));

        return new Node.FunDefinition(funType, parameters, fun.name(), fun.params(), inferredBody);
    }

    private static Node inferValNode(Environment environment, com.github.imaqtkatt.lang.tree.Node.ValDefinition val) {
        Environment newEnvironment = environment.clone();

        var inferredVal = InferExpression.infer(newEnvironment, val.value());
        environment.declareVal(val.name(), inferredVal.type());

        return new Node.ValDefinition(inferredVal.type(), val.name(), inferredVal);
    }
}
