package com.github.imaqtkatt.lang.compiler;

import com.github.imaqtkatt.lang.parser.Scope;
import com.github.imaqtkatt.lang.typed.Type;
import com.github.imaqtkatt.lang.typed.tree.Node;
import com.github.imaqtkatt.lang.typed.tree.Program;
import com.github.imaqtkatt.lang.typed.tree.TypedExpression;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.imaqtkatt.lang.compiler.BooleanHelpers.unwrapBoolean;
import static com.github.imaqtkatt.lang.compiler.BooleanHelpers.wrapBoolean;
import static com.github.imaqtkatt.lang.compiler.IntegerHelpers.unwrapInteger;
import static com.github.imaqtkatt.lang.compiler.IntegerHelpers.wrapInteger;

public final class Compiler {
    private final Program program;
    private final String className;

    static final String MUTABLE = "choco/lang/Mutable";
    static final String MUTABLE_SET_DESCRIPTOR = "(Lchoco/lang/Mutable;Ljava/lang/Object;)V";
    static final String MUTABLE_DEREF_DESCRIPTOR = "(Lchoco/lang/Mutable;)Ljava/lang/Object;";
    static final String MUTABLE_OF_DESCRIPTOR = "(Ljava/lang/Object;)Lchoco/lang/Mutable;";

    public Compiler(com.github.imaqtkatt.lang.typed.tree.Program program) {
        this.program = program;
        this.className = program.packageName() + "/" + "Main";
    }

    public byte[] compile() throws IOException {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        writer.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                className,
                null,
                "java/lang/Object",
                null
        );
        writer.visitSource(className + ".java", null);

        compileDefaultConstructor(writer);

        compileVals(writer);

        compileProgram(writer);

        return writer.toByteArray();
    }

    private void compileProgram(ClassWriter writer) {
        for (var node : program.definitions()) {
            if (node instanceof Node.FunDefinition fun) {
                compileFun(writer, fun);
            }
            if (node instanceof Node.ValDefinition val) {
                compileVal2(writer, val);
            }
        }
    }

    private void compileVals(ClassWriter writer) {
        var methodVisitor = writer.visitMethod(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );
        for (var node : program.definitions()) {
            if (node instanceof Node.ValDefinition val) {
                compileVal(methodVisitor, val);
            }
        }
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private static void compileDefaultConstructor(ClassWriter writer) {
        var methodVisitor = writer.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        );
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        );
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private void compileVal(MethodVisitor methodVisitor, Node.ValDefinition val) {
        compileExpression(methodVisitor, Map.of(), val.value());
        methodVisitor.visitFieldInsn(
                Opcodes.PUTSTATIC,
                className,
                val.name(),
                val.type().javaDescriptor()
        );
    }

    private void compileFun(ClassWriter writer, Node.FunDefinition fun) {
        var descriptor = fun.type().javaDescriptor();
        var methodVisitor = writer.visitMethod(
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                fun.name(),
                !fun.name().equals("lambda") ? descriptor : "()Lchoco/lang/Function;",
                !fun.name().equals("lambda") ? fun.type().signature() : null,
                null
        );
        var body = fun.body();
        var vars = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < fun.params().size(); i++) {
            var param = fun.params().get(i);
            var paramType = fun.paramsTypes().get(i);
            vars.computeIfAbsent(param, (_) -> vars.size());
            var label = new Label();
            methodVisitor.visitLocalVariable(
                    param,
                    paramType.javaDescriptor(),
                    paramType.signature(),
                    label,
                    label,
                    vars.get(param)
            );
        }

        compileExpression(methodVisitor, vars, body);
        var retDescriptor = fun.body().type().javaDescriptor();
        if (retDescriptor.equals(Type.VOID.javaDescriptor())) {
            methodVisitor.visitInsn(Opcodes.RETURN);
        } else {
            methodVisitor.visitInsn(Opcodes.ARETURN);
        }
        methodVisitor.visitFrame(
                Opcodes.F_SAME,
                0,
                new Object[]{},
                0,
                new Object[]{}
        );
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    private void compileExpression(MethodVisitor methodVisitor, Map<String, Integer> vars, TypedExpression e) {
        switch (e) {
            case TypedExpression.Binary binary -> {
                switch (binary.op()) {
                    case Add, Sub, Mul, Div -> compileArithmetic(methodVisitor, vars, binary);
                    case Set -> compileSet(methodVisitor, vars, binary);
                    case LT, GT, LE, GE, Eql -> compileComparison(methodVisitor, vars, binary);
                    case And, Or -> compileLogical(methodVisitor, vars, binary);
                }
            }
            case TypedExpression.Bool(Type ignored, Boolean b) -> {
                methodVisitor.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
                wrapBoolean(methodVisitor);
            }
            case TypedExpression.Call call -> {
                for (var arg : call.arguments()) {
                    compileExpression(methodVisitor, vars, arg);
                }
                if (call.callee() instanceof TypedExpression.Variable(
                        Type type, String name, Scope.DeclarationType decType
                ) && decType == Scope.DeclarationType.Fun) {
                    methodVisitor.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            className,
                            name,
                            type.javaDescriptor(),
                            false
                    );
                } else {
                    throw new IllegalStateException();
                }
            }
            case TypedExpression.Deref(Type type, TypedExpression mutable) -> {
                compileExpression(methodVisitor, vars, mutable);
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, MUTABLE);
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        MUTABLE,
                        "deref",
                        MUTABLE_DEREF_DESCRIPTOR,
                        false
                );
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, type.javaType());
            }
            case TypedExpression.Int(Type ignored, Integer i) -> {
                methodVisitor.visitLdcInsn(i);
                wrapInteger(methodVisitor);
            }
            case TypedExpression.Let let -> {
                vars.computeIfAbsent(let.bind(), (_) -> vars.size());
                var label = new Label();
                var value = let.value();
                methodVisitor.visitLocalVariable(
                        let.bind(),
                        value.type().javaDescriptor(),
                        value.type().signature(),
                        label,
                        label,
                        vars.get(let.bind())
                );
                compileExpression(methodVisitor, vars, let.value());
                methodVisitor.visitVarInsn(Opcodes.ASTORE, vars.get(let.bind()));
                compileExpression(methodVisitor, vars, let.body());
            }
            case TypedExpression.Mutable(Type ignored, TypedExpression base) -> {
                compileExpression(methodVisitor, vars, base);
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        MUTABLE,
                        "of",
                        MUTABLE_OF_DESCRIPTOR,
                        false
                );
            }
            case TypedExpression.Seq seq -> {
                compileExpression(methodVisitor, vars, seq.left());
                compileExpression(methodVisitor, vars, seq.right());
            }
            case TypedExpression.Variable variable -> {
                switch (variable.decType()) {
                    case Val -> methodVisitor.visitFieldInsn(
                            Opcodes.GETSTATIC,
                            className,
                            variable.name(),
                            variable.type().javaDescriptor()
                    );

                    case Fun -> throw new UnsupportedOperationException("Can't use function as reference.");

                    case Var -> {
                        var index = vars.get(variable.name());
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, index);
                    }
                }
            }
            case TypedExpression.If ifExpression -> {
                compileExpression(methodVisitor, vars, ifExpression.condition());
                unwrapBoolean(methodVisitor);

                var thenLabel = new Label();
                var otherwiseLabel = new Label();
                var end = new Label();

                // if zero
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, otherwiseLabel);

                methodVisitor.visitLabel(thenLabel);
                compileExpression(methodVisitor, vars, ifExpression.then());
                methodVisitor.visitJumpInsn(Opcodes.GOTO, end);

                methodVisitor.visitLabel(otherwiseLabel);
                compileExpression(methodVisitor, vars, ifExpression.otherwise());
                methodVisitor.visitJumpInsn(Opcodes.GOTO, end);

                methodVisitor.visitLabel(end);
            }
            case TypedExpression.Lambda _ -> throw new UnsupportedOperationException("Can't compile lambda");
        }
    }

    private void compileVal2(ClassWriter writer, com.github.imaqtkatt.lang.typed.tree.Node.ValDefinition val) {
        writer.visitField(
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                val.name(),
                val.type().javaDescriptor(),
                val.type().signature(),
                null
        );
    }

    private void compileArithmetic(MethodVisitor methodVisitor, Map<String, Integer> vars, TypedExpression.Binary binary) {
        compileExpression(methodVisitor, vars, binary.left());
        unwrapInteger(methodVisitor);
        compileExpression(methodVisitor, vars, binary.right());
        unwrapInteger(methodVisitor);
        switch (binary.op()) {
            case Add -> methodVisitor.visitInsn(Opcodes.IADD);
            case Sub -> methodVisitor.visitInsn(Opcodes.ISUB);
            case Mul -> methodVisitor.visitInsn(Opcodes.IMUL);
            case Div -> methodVisitor.visitInsn(Opcodes.IDIV);
            default -> throw new IllegalStateException();
        }
        wrapInteger(methodVisitor);
    }

    private void compileSet(MethodVisitor methodVisitor, Map<String, Integer> vars, TypedExpression.Binary binary) {
        compileExpression(methodVisitor, vars, binary.left());
        compileExpression(methodVisitor, vars, binary.right());
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                MUTABLE,
                "set",
                MUTABLE_SET_DESCRIPTOR,
                false
        );
    }

    private void compileComparison(MethodVisitor methodVisitor, Map<String, Integer> vars, TypedExpression.Binary binary) {
        compileExpression(methodVisitor, vars, binary.left());
        unwrapInteger(methodVisitor);

        compileExpression(methodVisitor, vars, binary.right());
        unwrapInteger(methodVisitor);

        var yes = new Label();
        var no = new Label();
        var end = new Label();

        switch (binary.op()) {
            case LT -> methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, yes);
            case GT -> methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, yes);
            case LE -> methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, yes);
            case GE -> methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, yes);
            case Eql -> methodVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, yes);
            default -> throw new IllegalStateException();
        }

        methodVisitor.visitLabel(no);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, end);

        methodVisitor.visitLabel(yes);
        methodVisitor.visitInsn(Opcodes.ICONST_1);
//                        methodVisitor.visitJumpInsn(Opcodes.GOTO, end);

        methodVisitor.visitLabel(end);
        wrapBoolean(methodVisitor);
    }

    /**
     * Compile logical `and` and `or` operations with short circuit evaluation.
     */
    private void compileLogical(MethodVisitor methodVisitor, Map<String, Integer> vars, TypedExpression.Binary binary) {
        compileExpression(methodVisitor, vars, binary.left());
        unwrapBoolean(methodVisitor);

        var returnFalse = new Label();
        var returnTrue = new Label();
        var wrapBoolean = new Label();

        switch (binary.op()) {
            case And -> methodVisitor.visitJumpInsn(Opcodes.IFEQ, returnFalse);
            case Or -> methodVisitor.visitJumpInsn(Opcodes.IFNE, returnTrue);
            default -> throw new IllegalStateException();
        }

        compileExpression(methodVisitor, vars, binary.right());
        unwrapBoolean(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, returnFalse);

        methodVisitor.visitLabel(returnTrue);
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, wrapBoolean);

        methodVisitor.visitLabel(returnFalse);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitLabel(wrapBoolean);
        wrapBoolean(methodVisitor);
    }
}
