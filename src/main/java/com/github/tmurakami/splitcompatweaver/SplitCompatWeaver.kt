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

import org.gradle.api.logging.Logging
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PROTECTED
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASM6
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.Opcodes.ICONST_0
import org.objectweb.asm.Opcodes.IFNE
import org.objectweb.asm.Opcodes.ILOAD
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.ISTORE
import org.objectweb.asm.Opcodes.POP
import org.objectweb.asm.Opcodes.RETURN

class SplitCompatWeaver(cv: ClassVisitor) : ClassVisitor(ASM6, cv) {
    private lateinit var className: String
    private var superClassName: String? = null
    private var shouldOverrideAttachBaseContext = true

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        superClassName = superName
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val visitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (isAttachBaseContext(name, descriptor)) {
            shouldOverrideAttachBaseContext = false
            object : MethodVisitor(api, visitor) {
                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                    isInterface: Boolean
                ) {
                    LOGGER.run {
                        if (isWarnEnabled &&
                            isSplitCompatInstallCall(opcode, owner, name, descriptor)
                        ) {
                            warn(
                                "Unnecessary call to 'SplitCompat#$INSTALL' in " +
                                    "${className.replace('/', '.')}#$ATTACH_BASE_CONTEXT"
                            )
                        }
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    if (isSuperAttachBaseContextCall(opcode, owner, name, descriptor)) {
                        mv.visitSplitCompatInstallCall()
                    }
                }

                override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                    super.visitMaxs(maxStack, maxLocals + 2)
                }
            }
        } else visitor
    }

    override fun visitEnd() {
        if (shouldOverrideAttachBaseContext) {
            super.visitMethod(
                ACC_PROTECTED,
                ATTACH_BASE_CONTEXT,
                ATTACH_BASE_CONTEXT_DESCRIPTOR,
                null,
                null
            ).run {
                visitCode()
                visitSuperAttachBaseContextCall()
                visitSplitCompatInstallCall()
                visitInsn(RETURN)
                visitMaxs(2, 4)
                visitEnd()
            }
        }
        super.visitEnd()
    }

    private fun isAttachBaseContext(name: String, descriptor: String): Boolean =
        name == ATTACH_BASE_CONTEXT && descriptor == ATTACH_BASE_CONTEXT_DESCRIPTOR

    private fun isSplitCompatInstallCall(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String
    ): Boolean = opcode == INVOKESTATIC &&
        owner == SPLIT_COMPAT &&
        name == INSTALL &&
        descriptor == INSTALL_DESCRIPTOR

    private fun isSuperAttachBaseContextCall(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String
    ): Boolean = opcode == INVOKESPECIAL &&
        owner == superClassName &&
        name == ATTACH_BASE_CONTEXT &&
        descriptor == ATTACH_BASE_CONTEXT_DESCRIPTOR

    private fun MethodVisitor.visitSuperAttachBaseContextCall() {
        visitVarInsn(ALOAD, 0)
        visitVarInsn(ALOAD, 1)
        visitMethodInsn(
            INVOKESPECIAL,
            superClassName,
            ATTACH_BASE_CONTEXT,
            ATTACH_BASE_CONTEXT_DESCRIPTOR,
            false
        )
    }

    private fun MethodVisitor.visitSplitCompatInstallCall() {
        val start = Label()
        val end = Label()
        val exceptionHandler = Label()
        visitTryCatchBlock(
            start, end, exceptionHandler,
            NO_CLASS_DEF_FOUND_ERROR
        )

        visitLabel(start)
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(
            INVOKESTATIC,
            INSTANT_APPS,
            IS_INSTANT_APP,
            IS_INSTANT_APP_DESCRIPTOR,
            false
        )
        visitVarInsn(ISTORE, 2)

        visitLabel(end)
        val testIfInstantApp = Label()
        visitJumpInsn(GOTO, testIfInstantApp)

        visitLabel(exceptionHandler)
        visitVarInsn(ASTORE, 3)
        visitInsn(ICONST_0)
        visitVarInsn(ISTORE, 2)

        visitLabel(testIfInstantApp)
        visitVarInsn(ILOAD, 2)
        val isInstantApp = Label()
        visitJumpInsn(IFNE, isInstantApp)
        visitVarInsn(ALOAD, 0)
        visitMethodInsn(
            INVOKESTATIC,
            SPLIT_COMPAT,
            INSTALL,
            INSTALL_DESCRIPTOR,
            false
        )
        visitInsn(POP)

        visitLabel(isInstantApp)
    }

    private companion object {
        private val LOGGER = Logging.getLogger(SplitCompatWeaver::class.java)
        private const val ATTACH_BASE_CONTEXT = "attachBaseContext"
        private const val ATTACH_BASE_CONTEXT_DESCRIPTOR = "(Landroid/content/Context;)V"
        private const val INSTANT_APPS = "com/google/android/instantapps/InstantApps"
        private const val IS_INSTANT_APP = "isInstantApp"
        private const val IS_INSTANT_APP_DESCRIPTOR = "(Landroid/content/Context;)Z"
        private const val SPLIT_COMPAT = "com/google/android/play/core/splitcompat/SplitCompat"
        private const val INSTALL = "install"
        private const val INSTALL_DESCRIPTOR = "(Landroid/content/Context;)Z"
        private const val NO_CLASS_DEF_FOUND_ERROR = "java/lang/NoClassDefFoundError"
    }
}
