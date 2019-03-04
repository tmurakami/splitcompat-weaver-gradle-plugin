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

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status.ADDED
import com.android.build.api.transform.Status.CHANGED
import com.android.build.api.transform.Status.NOTCHANGED
import com.android.build.api.transform.Status.REMOVED
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.github.tmurakami.splitcompatweaver.Action.COPY
import com.github.tmurakami.splitcompatweaver.Action.DELETE
import com.github.tmurakami.splitcompatweaver.Action.WEAVE
import org.gradle.api.logging.Logging
import java.io.File
import java.util.EnumSet
import javax.xml.parsers.SAXParserFactory

class ClassTransform(private val extension: AppExtension) : Transform() {
    override fun isIncremental(): Boolean = true
    override fun getName(): String = NAME
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> = SCOPES
    override fun getInputTypes(): Set<QualifiedContent.ContentType> = INPUT_TYPES

    override fun transform(invocation: TransformInvocation) {
        super.transform(invocation)
        val names = collectComponentNames(invocation.context.variantName)
        val isIncremental = invocation.isIncremental
        val outputDir = invocation.outputProvider
            .apply { if (!isIncremental) deleteAll() }
            .getContentLocation(name, inputTypes, scopes, Format.DIRECTORY)
        invocation.inputs.asSequence()
            .flatMap { it.directoryInputs.asSequence() }
            .flatMap { input ->
                val dir = input.file
                val root = "${dir.canonicalPath}/"
                if (isIncremental) {
                    input.changedFiles.asSequence()
                        .filterNot { (_, status) -> status == NOTCHANGED }
                        .map { (source, status) -> Triple(root, source, status) }
                } else {
                    dir.walk().filter { it.isFile }.map { Triple(root, it, ADDED) }
                }
            }.forEach { (root, source, status) ->
                LOGGER.run { if (isDebugEnabled) debug("Status [$status] $source") }
                val path = source.canonicalPath.removePrefix(root)
                when (status!!) {
                    NOTCHANGED -> null
                    REMOVED -> DELETE
                    ADDED, CHANGED -> {
                        if (names.contains(path.removeSuffix(".class"))) WEAVE else COPY
                    }
                }?.let {
                    val target = File(outputDir, path)
                    LOGGER.run { if (isDebugEnabled) debug("Action [$it] $target") }
                    it.invoke(source, target)
                }
            }
    }

    private fun collectComponentNames(variantName: String): Set<String> {
        val manifest = extension.applicationVariants
            .single { it.name == variantName }
            .outputs
            .single()
            .processManifestProvider
            .get()
            .metadataFeatureManifestOutputDirectory
            .walk()
            .single { it.name == SdkConstants.ANDROID_MANIFEST_XML }
        LOGGER.run { if (isDebugEnabled) debug("manifest: $manifest") }
        val names = mutableSetOf<String>()
        val collector = ComponentNameCollector(manifest, names)
        SAXParserFactory.newInstance().newSAXParser().parse(manifest, collector)
        return names
    }

    private companion object {
        private val LOGGER = Logging.getLogger(ClassTransform::class.java)
        private const val NAME = "splitCompatWeaver"
        private val SCOPES = EnumSet.of(QualifiedContent.Scope.PROJECT)!!
        private val INPUT_TYPES = EnumSet.of(QualifiedContent.DefaultContentType.CLASSES)!!
    }
}
