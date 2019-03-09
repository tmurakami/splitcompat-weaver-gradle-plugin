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

import org.gradle.api.logging.Logging
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File

internal class ComponentNameCollector(
    private val manifest: File,
    private val names: MutableSet<String>
) : DefaultHandler() {
    private lateinit var packageName: String

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes
    ) {
        super.startElement(uri, localName, qName, attributes)
        when (qName) {
            "manifest" -> packageName = attributes.getValue("package")
            "activity", "service" -> {
                val name = attributes.getValue("android:name")
                require(!name.startsWith("\${")) {
                    "The 'android:name' must not be a variable: $manifest"
                }
                val className = if (name[0] == '.') packageName + name else name
                names += className
                LOGGER.run { if (isDebugEnabled) debug("Target $qName: $className") }
            }
        }
    }

    private companion object {
        private val LOGGER = Logging.getLogger(ComponentNameCollector::class.java)
    }
}
