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

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_DEBUG
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class ActionTest {
    @[Rule JvmField]
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun copy() {
        val target = folder.newFile()
        Copy(SOURCE)(target)
        assertThat(target.readBytes()).isEqualTo(SOURCE.readBytes())
    }

    @Test
    fun delete() {
        val target = folder.newFile()
        assertThat(target.exists()).isTrue()
        Delete(target)
        assertThat(target.exists()).isFalse()
    }

    @Test
    fun weave() {
        val install = "com/google/android/play/core/splitcompat/SplitCompat.install"
        val flags = SKIP_DEBUG or SKIP_FRAMES
        assertThat(SOURCE.inputStream().use { i ->
            StringWriter().also { ClassReader(i).accept(TraceClassVisitor(PrintWriter(it)), flags) }
        }.toString()).doesNotContain(install)
        val target = folder.newFile()
        Weave(SOURCE)(target)
        assertThat(target.inputStream().use { i ->
            StringWriter().also { ClassReader(i).accept(TraceClassVisitor(PrintWriter(it)), flags) }
        }.toString()).contains(install)
    }

    private companion object {
        private val PATH = "/${TestActivity1::class.java.name.replace('.', '/')}.class"
        private val SOURCE = File(ActionTest::class.java.getResource(PATH).path)
    }
}
