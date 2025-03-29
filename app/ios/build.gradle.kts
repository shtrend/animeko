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

fun ipaArguments(
    destination: String = "generic/platform=iOS",
    sdk: String = "iphoneos",
): Array<String> {
    return arrayOf(
        "xcodebuild",
        "-workspace", "Animeko.xcworkspace",
        "-scheme", "Animeko",
        "-destination", destination,
        "-sdk", sdk,
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

/// FOR DEBUG

fun simulatorAppPath(): Provider<RegularFile> =
    // Adjust this path if your build output is located differently.
    layout.buildDirectory.file("Debug-iphonesimulator/Animeko.app")

tasks.register("buildDebugForSimulator", Exec::class) {
    group = "build"
    description = "Builds a debug version of Animeko for iOS Simulator"
    val destination = project.getLocalProperty("ani.ios.simulator.destination") ?: "generic/platform=iOS Simulator"
    inputs.property("destination", destination)
    workingDir(projectDir)
    commandLine(
        *ipaArguments(
            destination = destination,
            sdk = "iphonesimulator",
        ),
        "-configuration", "Debug",
        "build",
    )
}

tasks.register("launchSimulator", Exec::class) {
    group = "run"
    description = "Launches the iOS simulator"

    // This opens the default iOS Simulator app on macOS. 
    // You can also explicitly boot a specific device via `xcrun simctl boot "iPhone 14"` 
    // if you want more control.
    commandLine("open", "-a", "Simulator")
}

tasks.register("installDebugOnSimulator", Exec::class) {
    group = "run"
    description = "Installs the debug build on the Simulator"
    dependsOn("buildDebugForSimulator", "launchSimulator")

    val appPath = simulatorAppPath()
    // Typically, you want to ensure the simulator is booted before installing.
    commandLine("xcrun", "simctl", "install", "booted", appPath.get().asFile.absolutePath)
}

tasks.register("launchAppOnSimulator", Exec::class) {
    group = "run"
    description = "Launches the Animeko app on the simulator"
    dependsOn("installDebugOnSimulator")

    commandLine("xcrun", "simctl", "launch", "booted", "org.openani.Animeko")
}
