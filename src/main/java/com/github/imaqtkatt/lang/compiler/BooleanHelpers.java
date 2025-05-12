package com.github.imaqtkatt.lang.compiler;

import com.github.imaqtkatt.lang.typed.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class BooleanHelpers {
    static void wrapBoolean(MethodVisitor methodVisitor) {
        final String valueOf = "valueOf";
        final String descriptor = "(Z)Ljava/lang/Boolean;";
//        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.BOOL.javaType());
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.BOOL.javaType(),
                valueOf,
                descriptor,
                false
        );
    }

    static void unwrapBoolean(MethodVisitor methodVisitor) {
        final String booleanValue = "booleanValue";
        final String descriptor = "()Z";
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.BOOL.javaType());
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Type.BOOL.javaType(),
                booleanValue,
                descriptor,
                false
        );
    }
}
