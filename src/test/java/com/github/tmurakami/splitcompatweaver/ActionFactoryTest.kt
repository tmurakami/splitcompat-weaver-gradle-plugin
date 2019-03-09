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
import kotlin.reflect.KClass

@RunWith(Theories::class)
class ActionFactoryTest {
    private val actionFactory = ActionFactory(emptySet())

    @Theory
    fun createClassAction(testData: Pair<Status, KClass<*>>) =
        testData.let { (status, actionClass) ->
            val file = File("")
            val action = actionFactory.createClassAction(file, status, file)
            assertThat(action).isInstanceOf(actionClass.java)
        }

    companion object {
        @[DataPoints JvmField]
        val MAPPING: Array<Pair<Status, KClass<*>>> = arrayOf(
            ADDED to ReplaceClassAction::class,
            CHANGED to ReplaceClassAction::class,
            REMOVED to DeleteAction::class,
            NOTCHANGED to NopAction::class
        )
    }
}
