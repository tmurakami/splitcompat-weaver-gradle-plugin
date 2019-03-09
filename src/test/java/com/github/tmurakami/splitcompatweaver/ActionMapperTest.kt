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
import com.google.common.truth.Truth.assertThat
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import java.io.File
import java.util.AbstractMap
import kotlin.reflect.KClass

@RunWith(Theories::class)
class ActionMapperTest {
    @Theory
    fun test(testData: Triple<Status, String, KClass<*>>) =
        testData.let { (status, classFile, actionClass) ->
            val rootDir = File("/")
            val outputDir = File("")
            val componentClasses = setOf(TARGET_COMPONENT_CLASS)
            val mapToAction = ActionMapperFactory(outputDir, componentClasses).createMapper(rootDir)
            val action = mapToAction(AbstractMap.SimpleEntry(File("/$classFile"), status))
            assertThat(action).isInstanceOf(actionClass.java)
        }

    companion object {
        private const val TARGET_COMPONENT_CLASS = "Foo.class"
        @[DataPoints JvmField]
        val TEST_DATA: Array<Triple<Status, String, KClass<*>>> =
            arrayOf(
                Triple(ADDED, TARGET_COMPONENT_CLASS, Weave::class),
                Triple(ADDED, "Bar.class", Copy::class),
                Triple(CHANGED, TARGET_COMPONENT_CLASS, Weave::class),
                Triple(CHANGED, "Bar.class", Copy::class),
                Triple(REMOVED, "Foo.class", Delete::class),
                Triple(NOTCHANGED, "Foo.class", Nop::class)
            )
    }
}
