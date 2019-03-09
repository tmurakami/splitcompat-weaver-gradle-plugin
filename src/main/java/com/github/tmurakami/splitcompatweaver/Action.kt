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
import org.objectweb.asm.ClassWriter
import java.io.File

internal sealed class Action : Runnable

internal class Copy(private val source: File, private val target: File) : Action() {
    override fun run() {
        val source = source
        val target = target
        source.copyTo(target, true)
        LOGGER.run { if (isDebugEnabled) debug("Copied $source to $target") }
    }

    private companion object {
        private val LOGGER = Logging.getLogger(Copy::class.java)
    }
}

internal class Delete(private val target: File) : Action() {
    override fun run() = target.let {
        it.delete()
        LOGGER.run { if (isDebugEnabled) debug("Deleted $it") }
    }

    private companion object {
        private val LOGGER = Logging.getLogger(Delete::class.java)
    }
}

internal object Nop : Action() {
    override fun run() = Unit
}

internal class Weave(private val source: File, private val target: File) : Action() {
    override fun run() = target.run {
        parentFile.run {
            check(isDirectory || mkdirs()) { "Cannot make directory: $this" }
        }
        writeBytes(source.inputStream().use {
            val cr = ClassReader(it)
            ClassWriter(cr, 0).apply { cr.accept(SplitCompatWeaver(this), 0) }.toByteArray()
        })
        LOGGER.run { if (isDebugEnabled) debug("Wove 'SplitCompat#install' call into $target") }
    }

    private companion object {
        private val LOGGER = Logging.getLogger(Weave::class.java)
    }
}
