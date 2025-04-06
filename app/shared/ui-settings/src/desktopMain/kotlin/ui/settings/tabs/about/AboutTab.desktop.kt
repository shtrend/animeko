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
import me.him188.ani.app.platform.DesktopContext
import me.him188.ani.app.platform.LocalContext
import java.awt.Desktop
import kotlin.system.exitProcess

@Composable
internal actual fun ColumnScope.PlatformDebugInfoItems() {
    val context = LocalContext.current
    FilledTonalButton(
        {
            Desktop.getDesktop().open((context as DesktopContext).logsDir)
//        below also works on macOS, not sure about Windows
//        KoinPlatform.getKoin().get<BrowserNavigator>()
//            .openBrowser(context, "file://" + (context as DesktopContext).logsDir.absolutePath.replace(" ", "%20"))
        },
    ) {
        Text("打开日志目录")
    }
    FilledTonalButton(
        {
            exitProcess(0)
        }
    ) {
        Text("exitProcess(0)")
    }
}
