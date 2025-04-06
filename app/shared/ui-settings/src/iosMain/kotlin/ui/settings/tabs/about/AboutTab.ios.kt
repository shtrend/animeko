/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.tabs.about


import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.io.files.Path
import me.him188.ani.app.ui.foundation.widgets.LocalToaster
import me.him188.ani.utils.io.absolutePath
import me.him188.ani.utils.io.inSystem
import me.him188.ani.utils.io.readText
import me.him188.ani.utils.logging.DefaultLoggerFactory
import me.him188.ani.utils.logging.IosLoggingConfigurator
import me.him188.ani.utils.logging.writer.DailyRollingFileLogWriter
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

@Composable
internal actual fun ColumnScope.PlatformDebugInfoItems() {
    val toaster = LocalToaster.current
    val uiViewController = LocalUIViewController.current

    FilledTonalButton(
        {
            val file = getTodayLogFile()
            if (file == null) {
                toaster.toast("未找到文件")
            } else {
                shareFile(file.inSystem.absolutePath, uiViewController)
            }
        },
    ) {
        Text("分享当日日志文件")
    }

    val clipboard = LocalClipboardManager.current
    FilledTonalButton(
        {
            val file = getTodayLogFile()
            if (file == null) {
                toaster.toast("未找到文件")
            } else {
                clipboard.setText(AnnotatedString(file.inSystem.readText()))
            }
        },
    ) {
        Text("复制当日日志内容 (很大)")
    }
}

private fun shareFile(filePath: String, uiViewController: UIViewController) {
    // 1. Create a NSURL from the local file path
    val fileURL = NSURL.fileURLWithPath(filePath)

    // 2. Create a UIActivityViewController with the file URL as an "activity item"
    val activityVC = UIActivityViewController(
        activityItems = listOf(fileURL),
        applicationActivities = null,
    )

    // 3. On iPad, UIActivityViewController must be presented as a popover,
    //    so configure the popover anchor to avoid crashes.
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        activityVC.popoverPresentationController?.sourceView = uiViewController.view
    }

    // 4. Present the share sheet from the top-level ViewController
    uiViewController.presentViewController(activityVC, animated = true, completion = null)
}


private fun getTodayLogFile(): Path? {
    val factory = IosLoggingConfigurator.factory as? DefaultLoggerFactory
    return factory
        ?.writers
        ?.filterIsInstance<DailyRollingFileLogWriter>()
        ?.firstOrNull()
        ?.getTodayLogFile()
}
