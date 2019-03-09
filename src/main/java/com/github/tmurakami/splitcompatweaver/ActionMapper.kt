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

import com.android.build.api.transform.Status
import com.android.build.api.transform.Status.ADDED
import com.android.build.api.transform.Status.CHANGED
import com.android.build.api.transform.Status.NOTCHANGED
import com.android.build.api.transform.Status.REMOVED
import java.io.File

internal interface ActionMapper {
    operator fun invoke(e: Map.Entry<File, Status>): Action
}

internal class ActionMapperFactory(
    private val outputDir: File,
    private val componentClasses: Set<String>
) {
    fun createMapper(rootDir: File): ActionMapper =
        object : ActionMapper {
            override fun invoke(e: Map.Entry<File, Status>): Action = e.let { (file, status) ->
                val path = file.toRelativeString(rootDir)
                return when (status) {
                    ADDED, CHANGED -> {
                        val target = outputDir.resolve(path)
                        if (path in componentClasses) {
                            Weave(file, target)
                        } else {
                            Copy(file, target)
                        }
                    }
                    NOTCHANGED -> Nop
                    REMOVED -> Delete(outputDir.resolve(path))
                }
            }
        }
}
