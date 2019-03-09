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
    fun test(activityClass: KClass<out TestActivity>) {
        val activity = activityClass.createSplitCompatWovenActivity()
        mockkStatic(SplitCompat::class)
        try {
            every { SplitCompat.install(activity) } returns true
            activity.attachBaseContext(mockk())
            activity.assertSuperAttachBaseContextCalled()
            verify { SplitCompat.install(activity) }
        } finally {
            unmockkStatic(SplitCompat::class)
        }
    }

    private fun KClass<out TestActivity>.createSplitCompatWovenActivity(): TestActivity {
        val cls = java
        val activity = cls.name
        val bytecode = cls.getResourceAsStream("/${activity.replace('.', '/')}.class").use {
            val cr = ClassReader(it)
            ClassWriter(cr, 0).apply { cr.accept(SplitCompatWeaver(this), 0) }.toByteArray()
        }
        val classLoader = object : ClassLoader(cls.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> =
                if (name == activity) {
                    defineClass(name, bytecode, 0, bytecode.size)
                } else {
                    super.loadClass(name, resolve)
                }
        }
        return ObjenesisStd(false).newInstance(classLoader.loadClass(activity)) as TestActivity
    }

    companion object {
        @[DataPoints JvmField]
        val TEST_DATA: Array<KClass<out TestActivity>> =
            arrayOf(TestActivity1::class, TestActivity2::class)
    }
}
