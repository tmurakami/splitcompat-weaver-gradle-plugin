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

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File

internal enum class Action {
    COPY {
        override fun invoke(source: File, target: File) {
            source.copyTo(target, overwrite = true)
        }
    },
    DELETE {
        override fun invoke(source: File, target: File) {
            target.delete()
        }
    },
    WEAVE {
        override fun invoke(source: File, target: File) = target.run {
            parentFile.run {
                check(isDirectory || mkdirs()) { "Cannot make directory: $this" }
            }
            writeBytes(source.inputStream().use { i ->
                val reader = ClassReader(i)
                val writer = ClassWriter(reader, 0)
                writer.also { reader.accept(SplitCompatWeaver(it), 0) }.toByteArray()
            })
        }
    };

    abstract operator fun invoke(source: File, target: File)
}
