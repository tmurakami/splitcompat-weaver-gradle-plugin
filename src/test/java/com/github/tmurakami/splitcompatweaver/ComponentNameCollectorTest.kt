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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.xml.sax.InputSource
import java.io.File
import javax.xml.parsers.SAXParserFactory

private val MANIFEST = """
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="a.b">
            |    <application>
            |        <activity android:name=".A1" />
            |        <activity android:name="x.y.A2" />
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

class ComponentNameCollectorTest {
    @Test
    fun testParseManifest() {
        val internalNames = hashSetOf<String>()
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val collector = ComponentNameCollector(internalNames, File(""))
        MANIFEST.reader().use { parser.parse(InputSource(it), collector) }
        assertThat(internalNames).containsExactly("a/b/A1", "x/y/A2")
    }

    @Test
    fun testParseInvalidManifest() {
        val path = "/foo/bar/app/src/main/AndroidManifest.xml"
        val parser = SAXParserFactory.newInstance().newSAXParser()
        val collector = ComponentNameCollector(hashSetOf(), File(path))
        assertThrows(
            "The 'android:name' must not be a variable: $path",
            IllegalArgumentException::class.java
        ) {
            INVALID_MANIFEST.reader().use { parser.parse(InputSource(it), collector) }
        }
    }
}
