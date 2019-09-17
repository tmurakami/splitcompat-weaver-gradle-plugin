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
import com.android.build.api.transform.Format.DIRECTORY
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES
import com.android.build.api.transform.QualifiedContent.Scope.PROJECT
import com.android.build.api.transform.Status.ADDED
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.logging.Logging
import java.util.EnumSet
import javax.xml.parsers.SAXParserFactory

private val LOGGER = Logging.getLogger(ClassTransform::class.java)

internal class ClassTransform(private val variants: Set<BaseVariant>) : Transform() {
    override fun getName(): String = "splitCompatWeaver"
    override fun getInputTypes(): Set<QualifiedContent.ContentType> = EnumSet.of(CLASSES)
    override fun getScopes(): MutableSet<QualifiedContent.Scope> = EnumSet.of(PROJECT)
    override fun isIncremental(): Boolean = true

    override fun transform(invocation: TransformInvocation) {
        super.transform(invocation)
        val incremental = invocation.isIncremental
        val outputProvider = invocation.outputProvider.apply { if (!incremental) deleteAll() }
        val candidates = collectComponentNamesFor(invocation.context.variantName)
        val actionFactory = ActionFactory(candidates)
        invocation.inputs.asSequence()
            .flatMap { it.directoryInputs.asSequence() }
            .flatMap { input ->
                val inDir = input.file
                val outDir = outputProvider.getContentLocation(input, DIRECTORY)
                if (incremental) {
                    input.changedFiles.asSequence().map { it.toPair() }
                } else {
                    inDir.walkBottomUp().filter { it.isFile }.map { it to ADDED }
                }.filter { it.first.name.endsWith(".class") }.map { (f, s) ->
                    actionFactory.createClassAction(f, s, outDir.resolve(f.relativeTo(inDir)))
                }
            }
            .forEach(Action::run)
    }

    private fun collectComponentNamesFor(variantName: String): Set<String> {
        LOGGER.run { if (isDebugEnabled) debug("variant: $variantName") }
        val mf = (variants.find { it.name == variantName } ?: return emptySet())
            .outputs.single()
            .processManifestProvider.get()
            .metadataFeatureManifestOutputDirectory
            .walkBottomUp().single { it.name == SdkConstants.ANDROID_MANIFEST_XML }
        LOGGER.run { if (isDebugEnabled) debug("manifest: $mf") }
        return hashSetOf<String>().also {
            SAXParserFactory.newInstance().newSAXParser().parse(mf, ComponentNameCollector(it, mf))
        }
    }

    private fun TransformOutputProvider.getContentLocation(
        content: QualifiedContent,
        format: Format
    ) = content.run { getContentLocation(file.path, contentTypes, scopes, format) }
}
