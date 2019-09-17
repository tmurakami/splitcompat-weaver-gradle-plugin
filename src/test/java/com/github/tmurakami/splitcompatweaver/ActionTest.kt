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
import org.objectweb.asm.Type
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private const val METHOD_INSTALL = "com/google/android/play/core/splitcompat/SplitCompat.install"
private val INTERNAL_NAME = Type.getInternalName(TestActivity1::class.java)
private val SOURCE = File(ActionTest::class.java.getResource("/$INTERNAL_NAME.class").path)

class ActionTest {
    @[Rule JvmField]
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun testDeleteAction() {
        val target = folder.newFile()
        assertThat(target.exists()).isTrue()
        DeleteAction(target).run()
        assertThat(target.exists()).isFalse()
    }

    @Test
    fun testReplaceClassAction_weave() {
        assertThat(SOURCE.dumpClass()).doesNotContain(METHOD_INSTALL)
        val target = folder.newFile()
        ReplaceClassAction(SOURCE, target, setOf(INTERNAL_NAME)).run()
        assertThat(target.dumpClass()).contains(METHOD_INSTALL)
    }

    @Test
    fun testReplaceClassAction_copy() {
        val target = folder.newFile()
        ReplaceClassAction(SOURCE, target, emptySet()).run()
        assertThat(target.readBytes()).isEqualTo(SOURCE.readBytes())
    }

    private fun File.dumpClass(): String {
        val writer = StringWriter()
        val cr = ClassReader(readBytes())
        cr.accept(TraceClassVisitor(PrintWriter(writer)), SKIP_DEBUG or SKIP_FRAMES)
        return writer.toString()
    }
}
