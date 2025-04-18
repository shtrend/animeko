/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")

    `ani-mpp-lib-targets`
    kotlin("plugin.serialization")
    org.jetbrains.kotlinx.atomicfu
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(compose.components.resources)
    }
    sourceSets.commonTest.dependencies {
    }
    sourceSets.androidMain.dependencies {
    }
    sourceSets.desktopMain.dependencies {
    }
}

android {
    namespace = "me.him188.ani.app.ui.lang"
}

compose.resources {
    publicResClass = true
    packageOfResClass = "me.him188.ani.app.ui.lang"
    customDirectory(
        "commonMain",
        project.provider {
            project.layout.projectDirectory.dir("src/androidMain/res")
        },
    )
}

val populateStringsLocales by tasks.registering(Copy::class) {
    group = "ani"
    description =
        "Populate string resources for all locales. This will copy values-zh-rHK/strings.xml to other locales speaking Traditional Chinese."

    val chtLocales = listOf(
        "values-zh-rMO",
        "values-zh-rSG",
    )
    destinationDir = file("src/androidMain/res")

    // Copy rTW to chtLocales
    for (locale in chtLocales) {
        from(file("src/androidMain/res/values-zh-rHK/strings.xml")) {
            into(locale)
            rename { "strings.xml" }
        }
    }
}

tasks.matching {
    // desktop, `generateResourceAccessorsForCommonMain`
    it.name.startsWith("convertXmlValueResources")
            || it.name.startsWith("copyNonXmlValueResources")
            // android, `generateDebugResources`
            || (it.name.startsWith("generate") && it.name.endsWith("Resources"))
            || it.name.startsWith("extractDeepLinks")
            || (it.name.startsWith("map") && it.name.endsWith("SourceSetPaths")) // mapReleaseSourceSetPaths
}.configureEach {
    dependsOn(populateStringsLocales)
}
