# splitcompat-weaver-gradle-plugin

[![CircleCI](https://circleci.com/gh/tmurakami/splitcompat-weaver-gradle-plugin.svg?style=shield)](https://circleci.com/gh/tmurakami/splitcompat-weaver-gradle-plugin)
[![Release](https://jitpack.io/v/tmurakami/splitcompat-weaver-gradle-plugin.svg)](https://jitpack.io/#tmurakami/splitcompat-weaver-gradle-plugin)

A Gradle plugin that weaves [`SplitCompat.install`](https://developer.android.com/reference/com/google/android/play/core/splitcompat/SplitCompat.html#install(android.content.Context)) into your
Activities and Services.

## Installation

First, add the following into your root `build.gradle`:

```groovy
buildscript {
    repositories {
        google()
        maven { url 'https://jitpack.io' }
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.4.0' // requires 3.4.0+
        classpath 'com.github.tmurakami:splitcompat-weaver-gradle-plugin:0.1.0'
    }
}
```

Next, add `com.google.android.play:core` artifact as an `api` scoped
dependency into your base module's `build.gradle`:

```
api 'com.google.android.play:core:1.4.1' // requires 1.4.1+
```

Finally, apply this plugin in your Dynamic Feature Module's `build.gradle`:

```groovy
apply plugin: 'com.android.dynamic-feature'
apply plugin: 'com.github.tmurakami.splitcompat-weaver'
```

## Limitations

- This plugin only affects the Activities and Services contained in your
module to which it is applied. The classes in the libraries on which
that module relies won't be rewritten.

## License

```
Copyright 2019 Tsuyoshi Murakami

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
