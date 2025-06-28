/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.account

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.him188.ani.app.data.repository.subject.SubjectCollectionRepository
import me.him188.ani.app.ui.foundation.AbstractViewModel
import me.him188.ani.app.ui.foundation.rememberAsyncHandler
import me.him188.ani.app.ui.settings.SettingsTab
import me.him188.ani.app.ui.settings.framework.components.TextItem
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BangumiSyncTabViewModel() : AbstractViewModel(), KoinComponent {
    private val subjectCollectionRepository: SubjectCollectionRepository by inject()

    suspend fun sync() {
        subjectCollectionRepository.performBangumiFullSync()
    }
}

/**
 * Bangumi 同步设置. 只有用户已经登录并且绑定了 Bangumi 才可以进入此 tab.
 */
@Composable
fun BangumiSyncTab(
    onClickBangumiSync: () -> Unit,
    isBangumiSyncing: Boolean,
    modifier: Modifier = Modifier,
) = SettingsTab(modifier) {
    Group({ Text("同步") }) {
        TextItem(
            title = {
                Text("下载 Bangumi 数据")
            },
            onClick = onClickBangumiSync,
            onClickEnabled = !isBangumiSyncing,
            description = {
                Text("将 Bangumi 的收藏数据下载到 Animeko 收藏服务。通常来说不需要进行这个操作，Animeko 能自动完成同步。仅在你有发现数据不一致的情况时才需要手动下载")
            },
            action = {
                if (isBangumiSyncing) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Icon(Icons.Rounded.SyncAlt, contentDescription = "Bangumi sync")
                }
            },
        )
    }
}

@Composable
fun BangumiSyncTab(
    vm: BangumiSyncTabViewModel = viewModel<BangumiSyncTabViewModel> { BangumiSyncTabViewModel() },
    modifier: Modifier = Modifier
) {
    val asyncHandler = rememberAsyncHandler()
    BangumiSyncTab(
        onClickBangumiSync = {
            asyncHandler.launch {
                vm.sync()
            }
        },
        isBangumiSyncing = asyncHandler.isWorking,
        modifier = modifier,
    )
}

