/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.window.WindowPlacement
import me.him188.ani.app.platform.Context
import me.him188.ani.app.platform.LocalDesktopContext
import me.him188.ani.app.platform.PlatformWindow
import me.him188.ani.app.platform.checkIsDesktop
import me.him188.ani.app.platform.window.WindowUtils
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.utils.platform.Platform


@Composable
actual fun isSystemInFullscreenImpl(): Boolean {
    val context = LocalDesktopContext.current
    if (LocalPlatform.current is Platform.Windows) {
        return LocalPlatformWindow.current.isUndecoratedFullscreen
    }
    // should be true
    val placement by rememberUpdatedState(context.windowState.placement)
    val isFullscreen by remember { derivedStateOf { placement == WindowPlacement.Fullscreen } }
    return isFullscreen
}

actual suspend fun Context.setRequestFullScreen(window: PlatformWindow, fullscreen: Boolean) {
    checkIsDesktop()
//    extraWindowProperties.undecorated = fullscreen // Exception in thread "main" java.awt.IllegalComponentStateException: The frame is displayable.

    // hi, 相信前人的智慧, 如果操作不当会导致某些 Windows 设备上全屏会白屏 (你的电脑不一定能复现)
    if (fullscreen) {
        WindowUtils.setUndecoratedFullscreen(window, windowState, true)
    } else {
        WindowUtils.setUndecoratedFullscreen(window, windowState, false)
    }
}

actual fun Context.setSystemBarVisible(visible: Boolean) {
}