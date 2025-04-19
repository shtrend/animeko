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
        patchInfoPlist,
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


val buildDebugArchive = tasks.register("buildDebugArchive", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Debug"
    workingDir(projectDir)

    val output = layout.buildDirectory.dir("archives/debug/Animeko.xcarchive")
    outputs.dir(output)
    dependsOn(":app:shared:application:linkPodDebugFrameworkIosArm64")
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration", "Debug",
        "-archivePath", output.get().asFile.absolutePath,
    )
}

val buildReleaseArchive = tasks.register("buildReleaseArchive", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Release"
    workingDir(projectDir)

    dependsOn(":app:shared:application:linkPodReleaseFrameworkIosArm64")
    val output = layout.buildDirectory.dir("archives/release/Animeko.xcarchive")
    outputs.dir(output)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration", "Release",
        "-archivePath", output.get().asFile.absolutePath,
    )
}


/**
 * Packages an unsigned IPA **and** injects an ad‑hoc signature so sideloaders can re‑sign it.
 *
 * This task is **configuration‑cache safe** – it does *not* capture the `Project` instance.
 */
/**
 * Packages an unsigned IPA **and** injects an ad‑hoc signature so sideloaders can re‑sign it.
 *
 * This task is **configuration‑cache safe** – it does *not* capture the `Project` instance.
 */
@CacheableTask
abstract class BuildIpaTask : DefaultTask() {

    /* -------------------------------------------------------------
     * Inputs / outputs
     * ----------------------------------------------------------- */

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val archiveDir: DirectoryProperty

    @get:OutputFile
    abstract val outputIpa: RegularFileProperty

    /* -------------------------------------------------------------
     * Services (injected)
     * ----------------------------------------------------------- */

    @get:Inject
    abstract val execOperations: ExecOperations

    /* -------------------------------------------------------------
     * Action
     * ----------------------------------------------------------- */

    @TaskAction
    fun buildIpa() {
        // 1. Locate the .app inside the .xcarchive
        val appDir = archiveDir.get().asFile.resolve("Products/Applications/Animeko.app")
        if (!appDir.exists())
            throw GradleException("Could not find Animeko.app in archive at: ${appDir.absolutePath}")

        // 2. Create temporary Payload directory and copy .app into it
        val payloadDir = File(temporaryDir, "Payload").apply { mkdirs() }
        val destApp = File(payloadDir, appDir.name)
        appDir.copyRecursively(destApp, overwrite = true)

        // 3. Inject placeholder (ad‑hoc) code signature so AltStore / SideStore accept it
        logger.lifecycle("[IPA] Ad‑hoc signing ${destApp.name} …")
        execOperations.exec {
            commandLine(
                "codesign", "--force", "--deep", "--sign", "-", "--timestamp=none",
                destApp.absolutePath,
            )
        }

        // 4. Zip Payload ⇒ .ipa using the system `zip` command
        //
        //    -r : recurse into directories
        //    -y : store symbolic links as the link instead of the referenced file
        //
        // The working directory is the temporary folder so the archive
        // has a top‑level "Payload/" directory (required for .ipa files).
        val zipFile = File(temporaryDir, "Animeko.zip")
        execOperations.exec {
            workingDir(temporaryDir)
            commandLine("zip", "-r", "-y", zipFile.absolutePath, "Payload")
        }

        // 5. Move to final location (with .ipa extension)
        outputIpa.get().asFile.apply {
            parentFile.mkdirs()
            delete()
            zipFile.renameTo(this)
        }

        logger.lifecycle("[IPA] Created ad‑hoc‑signed IPA at: ${outputIpa.get().asFile.absolutePath}")
    }
}


tasks.register("buildDebugIpa", BuildIpaTask::class) {
    description = "Manually packages the .app from the .xcarchive into an unsigned .ipa"
    group = "build"

    // Adjust these paths as needed
    archiveDir = layout.buildDirectory.dir("archives/debug/Animeko.xcarchive")
    outputIpa = layout.buildDirectory.file("archives/debug/Animeko.ipa")
    dependsOn(buildDebugArchive)
}

tasks.register("buildReleaseIpa", BuildIpaTask::class) {
    description = "Manually packages the .app from the .xcarchive into an unsigned .ipa"
    group = "build"

    // Adjust these paths as needed
    archiveDir = layout.buildDirectory.dir("archives/release/Animeko.xcarchive")
    outputIpa = layout.buildDirectory.file("archives/release/Animeko.ipa")
    dependsOn(buildReleaseArchive)
}

//tasks.register("buildDebugIpa", Exec::class) {
//    val archiveFile = buildDebugArchive.get().outputs.files.singleFile
//    val output = projectDir.resolve("build/archives/Animeko.ipa")
//    outputs.file(output)
//    commandLine(
//        "xcodebuild",
//        "-exportArchive",
//        "-archivePath", archiveFile.absolutePath,
//        "-exportPath", output.absolutePath,
//        "-exportOptionsPlist", projectDir.resolve("exportOptions.plist").absolutePath,
//        "CODE_SIGNING_ALLOWED=NO",
//        "CODE_SIGNING_REQUIRED=NO",
//    )
//}


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

val patchInfoPlist = tasks.register("patchInfoPlist") {
    group = "run"
    description = "Patches Info.plist"

    val versionName = project.property("version.name").toString().substringBefore("-")
    inputs.property("version.name", versionName)
    val versionCode = project.property("android.version.code")
    inputs.property("version.code", versionCode)

    val templateFile = file("Animeko/Info.plist.template.txt")
    val outputFile = file("Animeko/Info.plist")
    inputs.file(templateFile)
    outputs.file(outputFile)

    doLast {
        val text = templateFile.readText()

        // Replace CFBundleShortVersionString with versionName
        // and CFBundleVersion with versionCode using a simple regex
        val updated = text
            .replace(
                Regex("""(<key>CFBundleShortVersionString</key>\s*<string>)([^<]+)(</string>)"""),
                "$1$versionName$3",
            )
            .replace(
                Regex("""(<key>CFBundleVersion</key>\s*<string>)([^<]+)(</string>)"""),
                "$1$versionCode$3",
            )

        if (updated != text) {
            outputFile.writeText(updated)
            logger.lifecycle("Patched Info.plist with versionName=$versionName and versionCode=$versionCode")
        } else {
            logger.lifecycle("No changes needed or did not match expected patterns in Info.plist.")
        }
    }
}

tasks.matching { it.path == ":app:shared:application:embedAndSignPodAppleFrameworkForXcode" }.configureEach {
    dependsOn(patchInfoPlist)
    inputs.file(file("Animeko/Info.plist"))
}
