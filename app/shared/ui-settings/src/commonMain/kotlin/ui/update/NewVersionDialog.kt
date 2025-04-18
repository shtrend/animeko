/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.ProvideCompositionLocalsForPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun NewVersionPopupCard(
    version: String,
    changes: List<String>,
    onDetailsClick: () -> Unit,
    onAutoUpdateClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(min = 280.dp, max = 380.dp),
        ) {
            /* ─── Title + Dismiss ─────────────────────────────────────────────── */
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "新版本",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            /* ─── Release Notes ──────────────────────────────────────────────── */
            Column {
                changes.forEach { change ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier,
                    ) {
                        Text("•")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            /* ─── Action Buttons ────────────────────────────────────────────── */
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Launch,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("查看详情") // See details
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = onAutoUpdateClick,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier,
                ) {
                    Text("自动更新") // Auto‑update
                }
            }
        }
    }
}

@Preview
@Composable
private fun NewVersionDialogPreview() {
    ProvideCompositionLocalsForPreview { // your project’s theme wrapper
        Surface {
            NewVersionPopupCard(
                version = "4.8.0‑alpha01",
                changes = listOf("支持标签搜索", "支持缓存在线源"),
                onDetailsClick = {},
                onAutoUpdateClick = {},
                onDismissRequest = {},
            )
        }
    }
}
