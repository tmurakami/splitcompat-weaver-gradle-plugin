/*
 * Copyright 2019 Tsuyoshi Murakami
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tmurakami.splitcompatweaver

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASM6
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.POP
import org.objectweb.asm.Opcodes.RETURN

internal class SplitCompatWeaver(cv: ClassVisitor) : ClassVisitor(ASM6, cv) {
    private lateinit var name: String
    private var superName: String? = null
    private var isWoven = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        this.name = name
        this.superName = superName
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (isWoven ||
            name != METHOD_ATTACH_BASE_CONTEXT ||
            descriptor != DESCRIPTOR_ATTACH_BASE_CONTEXT
        ) mv else SplitCompatInstallAdder(mv).also { isWoven = true }
    }

    override fun visitEnd() {
        if (!isWoven) {
            super.visitMethod(
                ACC_PROTECTED,
                METHOD_ATTACH_BASE_CONTEXT,
                DESCRIPTOR_ATTACH_BASE_CONTEXT,
                null,
                null
            ).run {
                visitCode()
                visitVarInsn(ALOAD, 0)
                visitVarInsn(ALOAD, 1)
                visitMethodInsn(
                    INVOKESPECIAL,
                    superName,
                    METHOD_ATTACH_BASE_CONTEXT,
                    DESCRIPTOR_ATTACH_BASE_CONTEXT,
                    false
                )
                visitSplitCompatInstallCall()
                visitInsn(RETURN)
                visitMaxs(2, 4)
                visitEnd()
            }
        }
        super.visitEnd()
    }

    private fun MethodVisitor.visitSplitCompatInstallCall() {
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(INVOKESTATIC, CLASS_SPLIT_COMPAT, METHOD_INSTALL, DESCRIPTOR_INSTALL, false)
        visitInsn(POP)
    }

    private inner class SplitCompatInstallAdder(mv: MethodVisitor) :
        MethodVisitor(api, mv) {
        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            if (opcode == INVOKESPECIAL &&
                owner == superName &&
                name == METHOD_ATTACH_BASE_CONTEXT &&
                descriptor == DESCRIPTOR_ATTACH_BASE_CONTEXT
            ) {
                mv.visitSplitCompatInstallCall()
            }
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack, maxLocals + 2)
        }
    }

    private companion object {
        private const val CLASS_CONTEXT = "android/content/Context"
        private const val METHOD_ATTACH_BASE_CONTEXT = "attachBaseContext"
        private const val DESCRIPTOR_ATTACH_BASE_CONTEXT = "(L$CLASS_CONTEXT;)V"
        private const val CLASS_SPLIT_COMPAT =
            "com/google/android/play/core/splitcompat/SplitCompat"
        private const val METHOD_INSTALL = "install"
        private const val DESCRIPTOR_INSTALL = "(L$CLASS_CONTEXT;)Z"
    }
}
