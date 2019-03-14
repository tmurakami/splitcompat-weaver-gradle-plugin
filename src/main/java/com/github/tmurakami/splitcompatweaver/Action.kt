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
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassReader.SKIP_DEBUG
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ASM6
import java.io.File

internal sealed class Action : Runnable

internal class DeleteAction(private val target: File) : Action() {
    override fun run() = target.let {
        it.delete()
        LOGGER.run { if (isDebugEnabled) debug("Deleted $it") }
    }

    private companion object {
        private val LOGGER = Logging.getLogger(DeleteAction::class.java)
    }
}

internal object NopAction : Action() {
    override fun run() = Unit
}

internal class ReplaceClassAction(
    private val source: File,
    private val target: File,
    private val candidates: Set<String>
) : Action() {
    override fun run() {
        val source = source
        val cr = ClassReader(source.readBytes())
        var needToWeave = false
        cr.accept(object : ClassVisitor(ASM6) {
            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                super.visit(version, access, name, signature, superName, interfaces)
                needToWeave = name in candidates
            }
        }, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)
        val target = target
        if (needToWeave) {
            target.parentFile.run {
                check(isDirectory || mkdirs()) { "Cannot make directory: $this" }
            }
            val cw = ClassWriter(cr, 0)
            cr.accept(SplitCompatWeaver(ASM6, cw), 0)
            target.writeBytes(cw.toByteArray())
        } else {
            source.copyTo(target, overwrite = true)
        }
        LOGGER.run { if (isDebugEnabled) debug("Replaced $target") }
    }

    private companion object {
        private val LOGGER = Logging.getLogger(ReplaceClassAction::class.java)
    }
}
