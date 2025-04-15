/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.settings.tabs.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import me.him188.ani.app.ui.foundation.a
import me.him188.ani.app.ui.foundation.ifNotNullThen
import me.him188.ani.app.ui.settings.Res
import me.him188.ani.app.ui.settings.btmuli
import me.him188.ani.app.ui.settings.him188
import me.him188.ani.app.ui.settings.jerryz233
import me.him188.ani.app.ui.settings.misakatat
import me.him188.ani.app.ui.settings.nekoouo
import me.him188.ani.app.ui.settings.nick
import me.him188.ani.app.ui.settings.nier4ever
import me.him188.ani.app.ui.settings.rdlwicked
import me.him188.ani.app.ui.settings.sanlorng
import me.him188.ani.app.ui.settings.stageguard
import me.him188.ani.app.ui.settings.woleoz
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DevelopersTab(
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxWidth()) {
        val listItemColors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        )

        AniHeroIconAndDescriptions()

        Spacer(Modifier.height(36.dp))

        @Composable
        fun LocalImage(
            res: DrawableResource,
            modifier: Modifier = Modifier
        ) {
            Image(
                painterResource(res),
                null,
                modifier.clip(CircleShape).size(48.dp),
            )
        }

        @Composable
        fun DeveloperItem(
            name: String,
            url: String?,
            avatar: @Composable () -> Unit,
            modifier: Modifier = Modifier,
            description: String? = null,
        ) {
            ListItem(
                headlineContent = { Text(name, style = MaterialTheme.typography.bodyLarge) },
                modifier = modifier.ifNotNullThen(url) {
                    clickable(onClick = { uriHandler.openUri(it) }, role = Role.Button)
                },
                leadingContent = { avatar() },
                supportingContent = description?.let { { Text(it) } },
                colors = listItemColors,
            )
        }

        @Composable
        fun Header(title: String) {
            ListItem(
                headlineContent = {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                colors = listItemColors,
            )
        }

        Header("主要贡献者")

        DeveloperItem(
            "Him188", "https://github.com/him188",
            description = "项目发起者",
            avatar = { LocalImage(Res.drawable.him188) },
        )

        DeveloperItem(
            "StageGuard", "https://github.com/StageGuard",
            description = "日常维护",
            avatar = { LocalImage(Res.drawable.stageguard) },
        )

        Header("卓越贡献者 (按字母排序)")

        DeveloperItem(
            "JerryZ233", "https://github.com/JerryZ233",
            description = "服务端开发",
            avatar = { LocalImage(Res.drawable.jerryz233) },
        )

        DeveloperItem(
            "MisakaTAT", "https://github.com/MisakaTAT",
            description = "Banugmi 上游贡献",
            avatar = { LocalImage(Res.drawable.misakatat) },
        )

        DeveloperItem(
            "NeKoOuO", "https://github.com/NeKoOuO",
            description = "图标绘制",
            avatar = { LocalImage(Res.drawable.nekoouo) },
        )

        DeveloperItem(
            "NickChenヰ", "https://github.com/nick-cjyx9",
            description = "官网开发",
            avatar = { LocalImage(Res.drawable.nick) },
        )

        DeveloperItem(
            "NieR4ever", "https://github.com/NieR4ever",
            description = "贡献者",
            avatar = { LocalImage(Res.drawable.nier4ever) },
        )

        DeveloperItem(
            "rdlwicked", "https://github.com/rdlwicked",
            description = "机器学习研发",
            avatar = { LocalImage(Res.drawable.rdlwicked) },
        )

        DeveloperItem(
            "Sanlorng", "https://github.com/Sanlorng",
            description = "贡献者",
            avatar = { LocalImage(Res.drawable.sanlorng) },
        )

        DeveloperItem(
            "WoLeo-Z", "https://github.com/WoLeo-Z",
            description = "贡献者",
            avatar = { LocalImage(Res.drawable.woleoz) },
        )

        DeveloperItem(
            "目棃", "https://github.com/BTMuli",
            description = "官网开发",
            avatar = { LocalImage(Res.drawable.btmuli) },
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        DeveloperItem(
            "OpenAni", "https://github.com/open-ani",
            description = "Organization",
            avatar = { LocalImage(me.him188.ani.app.ui.foundation.Res.drawable.a) },
        )

        ListItem(
            headlineContent = { Text("在 GitHub 上查看更多") },
            modifier = Modifier.clickable {
                uriHandler.openUri("https://github.com/open-ani/animeko/graphs/contributors")
            },
            trailingContent = {
                Icon(Icons.Rounded.ArrowOutward, null)
            },
            colors = listItemColors,
        )
    }
}

@Composable
@Preview
private fun PreviewDevelopersTab() = ProvideCompositionLocalsForPreview {
    Surface {
        DevelopersTab()
    }
}
