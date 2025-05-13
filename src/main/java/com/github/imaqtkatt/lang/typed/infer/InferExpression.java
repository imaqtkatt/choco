package com.github.imaqtkatt.lang.typed.infer;

import com.github.imaqtkatt.lang.parser.Scope;
import com.github.imaqtkatt.lang.tree.Expression;
import com.github.imaqtkatt.lang.tree.Operation;
import com.github.imaqtkatt.lang.typed.*;
import com.github.imaqtkatt.lang.typed.tree.TypedExpression;

import java.util.ArrayList;

import static com.github.imaqtkatt.lang.typed.Unify.unify;

public final class InferExpression {
    public static TypedExpression infer(Environment environment, Expression e) {
        return switch (e) {
            case Expression.Variable(String name, Scope.DeclarationType decType) -> {
                var type = environment.fetch(name);
                if (type.isEmpty()) {
                    throw new RuntimeException("Unbound variable '%s'".formatted(name));
                }
                yield new TypedExpression.Variable(type.get().instantiate(), name, decType);
            }

            case Expression.Binary binary -> switch (binary.op()) {
                case Add, Sub, Mul, Div -> inferArithmetic(environment, binary.op(), binary.left(), binary.right());
                case Set -> inferSet(environment, binary.left(), binary.right());
                case LT, GT, LE, GE, Eql -> inferCompare(environment, binary.op(), binary.left(), binary.right());
                case And, Or -> inferLogical(environment, binary.op(), binary.left(), binary.right());
            };

            case Expression.Call call -> inferCall(environment, call);

            case Expression.Deref deref -> inferDeref(environment, deref);

            case Expression.Let let -> inferLet(environment, let);

            case Expression.Mutable mutable -> inferMutable(environment, mutable);

            case Expression.Seq seq -> inferSeq(environment, seq);

            case Expression.If ifExpression -> inferIf(environment, ifExpression);

            case Expression.Lambda lambda -> inferLambda(environment, lambda);

            case Expression.Int(Integer i) -> new TypedExpression.Int(Type.INT, i);
            case Expression.Bool(Boolean b) -> new TypedExpression.Bool(Type.BOOL, b);
        };
    }

    private static Type.Mutable newMutable() {
        return new Type.Mutable(HoleGen.newHole());
    }

    private static TypedExpression inferArithmetic(Environment environment, Operation op, Expression left, Expression right) {
        var inferredLeft = infer(environment, left);
        unify(Type.INT, inferredLeft.type(), false);

        var inferredRight = infer(environment, right);
        unify(Type.INT, inferredRight.type(), false);

        return new TypedExpression.Binary(Type.INT, inferredLeft, op, inferredRight);
    }

    private static TypedExpression inferSet(Environment environment, Expression left, Expression right) {
        var inferredLeft = infer(environment, left);
        var mutableType = newMutable();
        unify(inferredLeft.type(), mutableType, false);

        var inferredRight = infer(environment, right);
        unify(inferredRight.type(), mutableType.inner(), false);

        return new TypedExpression.Binary(Type.VOID, inferredLeft, Operation.Set, inferredRight);
    }

    private static TypedExpression inferCall(Environment environment, Expression.Call call) {
        var callee = call.callee();
        var arguments = call.arguments();

        var inferredCallee = infer(environment, callee);
//        System.out.println("inferredCallee.type() = " + inferredCallee.type());

        var inferredArguments = arguments.stream().map((arg) -> infer(environment, arg)).toList();
        var inferredArgumentTypes = inferredArguments.stream().map(TypedExpression::type).toList();
        var retType = HoleGen.newHole();
        var funType = new Type.Fun(inferredArgumentTypes, retType);

        unify(inferredCallee.type(), funType, true);

        return new TypedExpression.Call(retType, inferredCallee, inferredArguments);
    }

    private static TypedExpression inferDeref(Environment environment, Expression.Deref deref) {
        var inferredE = infer(environment, deref.mutable());
        var mutableType = newMutable();
        unify(mutableType, inferredE.type(), false);

        return new TypedExpression.Deref(mutableType.inner(), inferredE);
    }

    private static TypedExpression inferLet(Environment environment, Expression.Let let) {
        HoleGen.enterLevel();
        var inferredValue = infer(environment, let.value());
        HoleGen.leaveLevel();

        // TODO: do this check in a better way
        if (Type.extract(inferredValue.type()) == Type.VOID) {
            throw new RuntimeException("Can't bind to void type");
        }

        // TODO: ???
        var newEnvironment = environment.clone();
        newEnvironment.insert(let.bind(), Scheme.ofType(inferredValue.type()));

        var inferredBody = infer(newEnvironment, let.body());

        return new TypedExpression.Let(inferredBody.type(), let.bind(), inferredValue, inferredBody);
    }

    private static TypedExpression inferMutable(Environment environment, Expression.Mutable mutable) {
        var inferredInner = infer(environment, mutable.base());
        var mutableType = new Type.Mutable(inferredInner.type());
        return new TypedExpression.Mutable(mutableType, inferredInner);
    }

    private static TypedExpression inferSeq(Environment environment, Expression.Seq seq) {
        var inferredLeft = infer(environment, seq.left());
        unify(Type.VOID, inferredLeft.type(), true);
        var inferredRight = infer(environment, seq.right());
        return new TypedExpression.Seq(inferredRight.type(), inferredLeft, inferredRight);
    }

    private static TypedExpression inferIf(Environment environment, Expression.If ifExpression) {
        var inferredCondition = infer(environment, ifExpression.condition());
        unify(Type.BOOL, inferredCondition.type(), false);

        var inferredThen = infer(environment, ifExpression.then());
        var inferredOtherwise = infer(environment, ifExpression.otherwise());
        unify(inferredThen.type(), inferredOtherwise.type(), true);

        return new TypedExpression.If(inferredThen.type(), inferredCondition, inferredThen, inferredOtherwise);
    }

    private static TypedExpression inferCompare(Environment environment, Operation op, Expression left, Expression right) {
        var inferredLeft = infer(environment, left);
        unify(Type.INT, inferredLeft.type(), false);

        var inferredRight = infer(environment, right);
        unify(Type.INT, inferredRight.type(), false);

        return new TypedExpression.Binary(Type.BOOL, inferredLeft, op, inferredRight);
    }

    private static TypedExpression inferLogical(Environment environment, Operation op, Expression left, Expression right) {
        var inferredLeft = infer(environment, left);
        unify(Type.BOOL, inferredLeft.type(), false);

        var inferredRight = infer(environment, right);
        unify(Type.BOOL, inferredRight.type(), false);

        return new TypedExpression.Binary(Type.BOOL, inferredLeft, op, inferredRight);
    }

    private static TypedExpression inferLambda(final Environment environment, Expression.Lambda lambda) {
        var newEnvironment = environment.clone();

        var paramTypes = new ArrayList<Type>();
        for (var param : lambda.parameters()) {
            var hole = HoleGen.newHole();
            paramTypes.addLast(hole);
            newEnvironment.insert(param, Scheme.ofType(hole));
        }

        var inferredBody = infer(newEnvironment, lambda.body());
        var inferredType = new Type.Fun(paramTypes, inferredBody.type());

        return new TypedExpression.Lambda(inferredType, lambda.parameters(), inferredBody);
    }
}
