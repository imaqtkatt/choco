package com.github.imaqtkatt.lang.compiler;

import com.github.imaqtkatt.lang.typed.Type;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class IntegerHelpers {
    static void wrapInteger(MethodVisitor methodVisitor) {
        final String valueOf = "valueOf";
        final String descriptor = "(I)Ljava/lang/Integer;";
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.INT.javaType(),
                valueOf,
                descriptor,
                false
        );
    }

    static void unwrapInteger(MethodVisitor methodVisitor) {
        final String intValue = "intValue";
        final String descriptor = "()I";

        // TODO: check if really needs this
         methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.INT.javaType());

        methodVisitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Type.INT.javaType(),
                intValue,
                descriptor,
                false
        );
    }
}
