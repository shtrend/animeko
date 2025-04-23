/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.platform

import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskAll

interface ViewControllerPropertyProvider {
    val prefersStatusBarHidden: Boolean
    val prefersHomeIndicatorAutoHidden: Boolean
    val supportedInterfaceOrientations: UIInterfaceOrientationMask
}

// Provides to Swift
object MainViewControllerPropertyProvider : ViewControllerPropertyProvider {
    override var prefersStatusBarHidden: Boolean = false
    override var prefersHomeIndicatorAutoHidden: Boolean = false
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask = UIInterfaceOrientationMaskAll
}

//fun getMainViewControllerPropertyProvider(): ViewControllerPropertyProvider {
//    return MainViewControllerPropertyProvider
//}
