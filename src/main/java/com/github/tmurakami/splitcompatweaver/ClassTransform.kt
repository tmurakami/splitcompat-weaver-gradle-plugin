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
import com.android.build.api.transform.Context
import com.android.build.api.transform.Format.DIRECTORY
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status.ADDED
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import org.gradle.api.logging.Logging
import java.util.AbstractMap
import java.util.EnumSet
import javax.xml.parsers.SAXParserFactory

internal class ClassTransform(private val extension: AppExtension) : Transform() {
    override fun isIncremental(): Boolean = true
    override fun getName(): String = NAME
    override fun getScopes(): MutableSet<QualifiedContent.Scope> = SCOPES
    override fun getInputTypes(): Set<QualifiedContent.ContentType> = INPUT_TYPES

    override fun transform(invocation: TransformInvocation) {
        super.transform(invocation)
        val incremental = invocation.isIncremental
        val outputDir = invocation.outputProvider
            .apply { if (!incremental) deleteAll() }
            .getContentLocation(name, inputTypes, scopes, DIRECTORY)
        val actionMapperFactory =
            ActionMapperFactory(outputDir, collectComponentClasses(invocation.context))
        invocation.inputs.asSequence()
            .flatMap { it.directoryInputs.asSequence() }
            .flatMap { input ->
                val rootDir = input.file
                val mapToAction = actionMapperFactory.createMapper(rootDir)
                if (incremental) {
                    input.changedFiles.asSequence()
                } else {
                    rootDir.walk().filter { it.isFile }.map { AbstractMap.SimpleEntry(it, ADDED) }
                }.map(mapToAction::invoke)
            }.forEach(Action::run)
    }

    private fun collectComponentClasses(context: Context): Set<String> {
        val mf = extension.applicationVariants.single { it.name == context.variantName }
            .outputs.single()
            .processManifestProvider.get()
            .metadataFeatureManifestOutputDirectory
            .walk().single { it.name == SdkConstants.ANDROID_MANIFEST_XML }
        LOGGER.run { if (isDebugEnabled) debug("manifest: $mf") }
        return mutableSetOf<String>().also {
            SAXParserFactory.newInstance().newSAXParser().parse(mf, ComponentNameCollector(mf, it))
        }.asSequence().map { it.replace('.', '/') + ".class" }.toHashSet()
    }

    private companion object {
        private val LOGGER = Logging.getLogger(ClassTransform::class.java)
        private const val NAME = "splitCompatWeaver"
        private val SCOPES = EnumSet.of(QualifiedContent.Scope.PROJECT)
        private val INPUT_TYPES = EnumSet.of(QualifiedContent.DefaultContentType.CLASSES)
    }
}
