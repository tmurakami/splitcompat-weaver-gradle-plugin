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
            val activity = activityClass.create(instantAppsNotFound)
            mockkStatic(InstantApps::class, SplitCompat::class)
            try {
                every { InstantApps.isInstantApp(activity) } returns false
                every { SplitCompat.install(activity) } returns true
                activity.attachBaseContext(mockk())
                activity.assertSuperAttachBaseContextCalled()
                verify(inverse = instantAppsNotFound) { InstantApps.isInstantApp(activity) }
                verify(exactly = installCalls) { SplitCompat.install(activity) }
            } finally {
                unmockkStatic(InstantApps::class, SplitCompat::class)
            }
        }
    }

    private fun KClass<out TestActivity>.create(instantAppsNotFound: Boolean): TestActivity {
        val cls = java
        val activity = cls.name
        val bytecode = cls.getResourceAsStream("/${activity.replace('.', '/')}.class").use {
            val cr = ClassReader(it)
            ClassWriter(cr, 0).apply { cr.accept(SplitCompatWeaver(this), 0) }.toByteArray()
        }
        val classLoader = object : ClassLoader(cls.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> =
                when {
                    instantAppsNotFound && name == InstantApps::class.java.name -> {
                        throw ClassNotFoundException(name)
                    }
                    name == activity -> defineClass(name, bytecode, 0, bytecode.size)
                    else -> super.loadClass(name, resolve)
                }
        }
        return ObjenesisStd(false).newInstance(classLoader.loadClass(activity)) as TestActivity
    }

    data class TestData(
        val activityClass: KClass<out TestActivity>,
        val installCalls: Int = 1
    )

    companion object {
        @[DataPoints JvmField]
        val TEST_DATA = arrayOf(
            TestData(TestActivity1::class),
            TestData(TestActivity2::class),
            TestData(TestActivity3::class, installCalls = 2)
        )
        @[DataPoints JvmField Suppress("BooleanLiteralArgument")]
        val INSTANT_APPS_NOT_FOUND = booleanArrayOf(false, true)
    }
}
