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
import org.junit.rules.ExpectedException
import org.xml.sax.InputSource
import java.io.File
import javax.xml.parsers.SAXParserFactory

class ComponentNameCollectorTest {
    @[Rule JvmField]
    val expectedException: ExpectedException = ExpectedException.none()!!

    @Test
    fun parseManifest() {
        val names = mutableSetOf<String>()
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val collector = ComponentNameCollector(File(""), names)
        MANIFEST.reader().use { parser.parse(InputSource(it), collector) }
        assertThat(names).containsExactly("a/b/A1", "x/y/A2", "a/b/S1", "x/y/z/S2")
    }

    @Test
    fun parseInvalidManifest() {
        val path = "/foo/bar/app/src/main/AndroidManifest.xml"
        expectedException.run {
            expect(IllegalArgumentException::class.java)
            expectMessage("The 'android:name' must not be a variable: $path")
        }
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val collector = ComponentNameCollector(File(path), mutableSetOf())
        INVALID_MANIFEST.reader().use { parser.parse(InputSource(it), collector) }
    }

    private companion object {
        private val MANIFEST = """
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="a.b">
            |    <application>
            |        <activity android:name=".A1" />
            |        <activity android:name="x.y.A2" />
            |        <service android:name=".S1"/>
            |        <service android:name="x.y.z.S2"/>
            |    </application>
            |</manifest>
            |""".trimMargin()
        private val INVALID_MANIFEST = """
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="a.b">
            |    <application>
            |        <activity android:name="${'$'}{activityName}" />
            |    </application>
            |</manifest>
            |""".trimMargin()
    }
}
