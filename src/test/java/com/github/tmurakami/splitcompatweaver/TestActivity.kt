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

import android.app.Activity
import android.content.Context
import com.google.common.truth.Truth.assertThat

open class TestActivity protected constructor() : Activity() {
    private var called = false

    public override fun attachBaseContext(newBase: Context?) {
        called = true
    }

    fun assertSuperAttachBaseContextCalled() = assertThat(called).isTrue()
}

class TestActivity1 : TestActivity()

class TestActivity2 : TestActivity() {
    @Suppress("RedundantOverride")
    override fun attachBaseContext(newBase: Context?) = super.attachBaseContext(newBase)
}
