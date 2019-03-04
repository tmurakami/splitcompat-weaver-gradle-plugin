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
import java.io.File

class ActionTest {
    @[Rule JvmField]
    val folder = TemporaryFolder()

    private val source = File(
        javaClass.getResource(
            "/${TestActivity1::class.java.name.replace('.', '/')}.class"
        ).path
    )

    @Test
    fun copy() {
        val target = folder.newFile()
        assertThat(target.delete()).isTrue()
        Action.COPY(source, target)
        assertThat(target.readBytes()).isEqualTo(source.readBytes())
    }

    @Test
    fun delete() {
        val target = folder.newFile()
        assertThat(target.exists()).isTrue()
        Action.DELETE(source, target)
        assertThat(target.exists()).isFalse()
    }

    @Test
    fun weave() {
        val target = folder.newFile()
        assertThat(target.delete()).isTrue()
        Action.WEAVE(source, target)
        assertThat(target.readBytes()).isNotEqualTo(source.readBytes())
    }
}
