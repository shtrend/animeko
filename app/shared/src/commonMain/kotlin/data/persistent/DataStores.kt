/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.data.persistent

import kotlinx.atomicfu.locks.SynchronizedObject
import me.him188.ani.app.platform.Context
import kotlin.concurrent.Volatile


/**
 * Must not be stored
 */
expect fun Context.createPlatformDataStoreManager(): PlatformDataStoreManager

// workaround for compiler bug
val Context.dataStores: PlatformDataStoreManager get() = PlatformDataStoreManagerHolder.init(this)

private object PlatformDataStoreManagerHolder : SynchronizedObject() {
    @Volatile
    private var instance: PlatformDataStoreManager? = null

    fun init(context: Context): PlatformDataStoreManager {
        instance?.let {
            return it
        }
        kotlinx.atomicfu.locks.synchronized(this) {
            check(instance == null) {
                "PlatformDataStoreManager already initialized with another Context instance"
            }
            instance?.let {
                return it
            }
            return context.createPlatformDataStoreManager().also {
                instance = it
            }
        }
    }
}
