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
        api(projects.app.shared.appPlatform)
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
