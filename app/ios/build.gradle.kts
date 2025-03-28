/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

tasks.register("podInstall", Exec::class) {
    group = "build"
    description = "Builds the iOS framework"
    dependsOn(
        ":app:shared:application:podspec",
    )

    workingDir(projectDir)
    commandLine("pod", "install")
}

tasks.register("iosFramework") {
    group = "build"
    description = "Builds the iOS framework"
    dependsOn(
        ":app:shared:application:embedAndSignPodAppleFrameworkForXcode",
    )
}
fun ipaArguments(): Array<String> {
    return arrayOf(
        "xcodebuild",
        "-workspace", "Animeko.xcworkspace",
        "-scheme", "Animeko",
        "-destination", "generic/platform=iOS",
        "-sdk", "iphoneos",
        "CODE_SIGNING_ALLOWED=NO",
        "CODE_SIGNING_REQUIRED=NO",
    )
}

tasks.register("buildDebugIpa", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Debug"
    workingDir(projectDir)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration", "Debug",
    )
}

tasks.register("buildReleaseIpa", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Release"
    workingDir(projectDir)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration", "Release",
    )
}
