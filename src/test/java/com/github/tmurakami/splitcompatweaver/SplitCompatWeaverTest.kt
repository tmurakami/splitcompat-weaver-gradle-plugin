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

import com.google.android.instantapps.InstantApps
import com.google.android.play.core.splitcompat.SplitCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objenesis.ObjenesisStd
import kotlin.reflect.KClass

@RunWith(Theories::class)
class SplitCompatWeaverTest {
    @Theory
    fun test(testData: TestData, instantAppsNotFound: Boolean) {
        testData.let { (activityClass, installCalls) ->
            val target = activityClass.weave(instantAppsNotFound)
            mockkStatic(InstantApps::class, SplitCompat::class)
            try {
                every { InstantApps.isInstantApp(target) } returns false
                every { SplitCompat.install(target) } returns true
                target.attachBaseContext(mockk())
                target.assertSuperAttachBaseContextCalled()
                verify(inverse = instantAppsNotFound) { InstantApps.isInstantApp(target) }
                verify(exactly = installCalls) { SplitCompat.install(target) }
            } finally {
                unmockkStatic(InstantApps::class, SplitCompat::class)
            }
        }
    }

    private fun KClass<out TestActivity>.weave(instantAppsNotFound: Boolean): TestActivity {
        val c = java
        val activityName = c.name
        val bytes = c.getResourceAsStream("/${c.name.replace('.', '/')}.class").use { i ->
            val reader = ClassReader(i)
            val writer = ClassWriter(reader, 0)
            writer.also { reader.accept(SplitCompatWeaver(it), 0) }.toByteArray()
        }
        val loader = object : ClassLoader() {
            override fun loadClass(name: String?, resolve: Boolean): Class<*> =
                if (instantAppsNotFound && name == InstantApps::class.java.name) {
                    throw ClassNotFoundException(name)
                } else if (name == activityName) {
                    defineClass(name, bytes, 0, bytes.size)
                } else {
                    super.loadClass(name, resolve)
                }
        }
        return ObjenesisStd().newInstance(loader.loadClass(activityName)) as TestActivity
    }

    data class TestData(
        val activityClass: KClass<out TestActivity>,
        val installCalls: Int = 1
    )

    companion object {
        @[DataPoints JvmField]
        val testData = arrayOf(
            TestData(TestActivity1::class),
            TestData(TestActivity2::class),
            TestData(
                TestActivity3::class,
                installCalls = 2
            )
        )
        @[DataPoints JvmField Suppress("BooleanLiteralArgument")]
        val instantAppsNotFound = booleanArrayOf(false, true)
    }
}
