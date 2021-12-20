package com.amazon.corretto.hotpatch.log4j2;

import com.amazon.corretto.hotpatch.HotPatch;
import com.amazon.corretto.hotpatch.Util;
import com.amazon.corretto.hotpatch.org.objectweb.asm.*;

public class Log4jDisableLiteralPatternConverter implements HotPatch {
    static final String CLASS_NAME = "org.apache.logging.log4j.core.pattern.LiteralPatternConverter";
    static final String CLASS_NAME_SLASH = CLASS_NAME.replace(".", "/");

    private final static String NAME = "Log4j2_DisableLiteralPatternConverter";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isValidClass(String className) {
        return className.endsWith(CLASS_NAME) || className.endsWith(CLASS_NAME_SLASH);
    }

    @Override
    public byte[] apply(byte[] classfileBuffer) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new DisableLiteralPatternConverterClassVisitor(cw);
        ClassReader cr = new ClassReader(classfileBuffer);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    public static class DisableLiteralPatternConverterClassVisitor extends ClassVisitor {

        public DisableLiteralPatternConverterClassVisitor(ClassVisitor classVisitor) {
            super(Util.asmApiVersion(), classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if ("format".equals(name)) {
                mv = new LiteralPatternConverterMethodVisitor(mv);
            }
            return mv;
        }
    }

    public static class LiteralPatternConverterMethodVisitor extends MethodVisitor implements Opcodes {
        private static final String OWNER = CLASS_NAME_SLASH;
        private static final String DESC = "Z";
        private static final String NAME = "substitute";
        enum State {
            CLEAR,
            LOADED_SUBSTITUTE,
        }

        private State state = State.CLEAR;

        public LiteralPatternConverterMethodVisitor(MethodVisitor methodVisitor) {
            super(Util.asmApiVersion(), methodVisitor);
        }

        @Override
        public void visitFieldInsn(int opc, String owner, String name, String desc) {
            if (OWNER.equals(owner) && NAME.equals(name) && DESC.equals(desc) && opc == GETFIELD) {
                visitState();
            } else {
                clearState();
            }
            mv.visitFieldInsn(opc, owner, name, desc);
        }

        @Override
        public void visitJumpInsn(int opc, Label label) {
            mv.visitJumpInsn(opc, label);
            if (state == State.LOADED_SUBSTITUTE && opc == IFEQ) {
                mv.visitJumpInsn(GOTO, label);
            }
            clearState();

        }

        private void clearState() {
            state = State.CLEAR;
        }

        private void visitState() {
            state = State.LOADED_SUBSTITUTE;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            clearState();
            mv.visitVarInsn(opcode, var);
        }

        @Override
        public void visitTypeInsn(int opcode, String desc) {
            clearState();
            mv.visitTypeInsn(opcode, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            clearState();
            mv.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitLabel(Label label) {
            mv.visitLabel(label);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            clearState();
            mv.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            clearState();
            mv.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            mv.visitMultiANewArrayInsn(desc, dims);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            mv.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            mv.visitLocalVariable(name, desc, signature, start, end, index);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            mv.visitLineNumber(line, start);
        }
    }
}
