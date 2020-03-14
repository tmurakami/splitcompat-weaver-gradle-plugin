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

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

private const val DYNAMIC_FEATURE = "com.android.dynamic-feature"
private const val DEPENDENCY_PLAY_CORE = "com.google.android.play:core:1.6.5"

internal class GradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target.plugins.hasPlugin(DYNAMIC_FEATURE)) {
            "Missing '$DYNAMIC_FEATURE' plugin"
        }
        val android = target.extensions.findByType(AppExtension::class.java)
        try {
            android!!.viewBinding // Added in the version 3.6.0
        } catch (t: Throwable) {
            throw IllegalArgumentException("Requires Android Gradle Plugin 3.6.0+")
        }
        android.registerTransform(ClassTransform(android.applicationVariants))
        target.configurations.configureEach {
            if (it.name == "implementation") {
                it.dependencies += target.dependencies.create(DEPENDENCY_PLAY_CORE)
            }
        }
    }
}
