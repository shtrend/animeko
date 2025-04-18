/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.him188.ani.app.ui.foundation.LocalPlatform
import me.him188.ani.app.ui.foundation.animation.AniAnimatedVisibility
import me.him188.ani.utils.platform.isDesktop

/**
 * A one‑stop host that
 *  1. kicks off automatic update check on the first composition, and
 *  2. shows *either* a bottom‑right popup (desktop) *or* a snackbar (mobile)
 *     when a new version is available.
 */
@Composable
fun UpdateNotifierHost(
    viewModel: AutoUpdateViewModel = viewModel { AutoUpdateViewModel() },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val platform = LocalPlatform.current
    val uriHandler = LocalUriHandler.current

    // ─── Start automatic check exactly once ───────────────────────────────────
    LaunchedEffect(Unit) { viewModel.startAutomaticCheckLatestVersion() }

    // track whether the user dismisses the notification in this session
    var dismissedManually by rememberSaveable { mutableStateOf(false) }

    val newVersion = viewModel.latestVersion

    Box(modifier) {
        // host the caller's UI first
        content()

        if (!dismissedManually && newVersion != null) {
            if (platform.isDesktop()) {
                DesktopPopup(
                    version = newVersion,
                    onDismiss = { dismissedManually = true },
                    onAutoUpdateClick = { viewModel.startDownload(newVersion, uriHandler) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
                        .shadow(4.dp, MaterialTheme.shapes.extraLarge),
                )
            } else {
                MobileSnackbar(
                    hostState = snackbarHostState,
                    version = newVersion,
                    dismissedManually = dismissedManually,
                    onAutoUpdateClick = { viewModel.startDownload(newVersion, uriHandler) },
                    onDismissChanged = { dismissedManually = it },
                )
            }
        }

        // Provide snackbar host on mobile so caller can place it.
        if (!platform.isDesktop()) {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

// ───────────────────────── Desktop implementation ────────────────────────────
@Composable
private fun BoxScope.DesktopPopup(
    version: NewVersion,
    onDismiss: () -> Unit,
    onAutoUpdateClick: () -> Unit,
    modifier: Modifier,
) {
    val uriHandler = LocalUriHandler.current

    AniAnimatedVisibility(
        visible = true,
        modifier = modifier.align(Alignment.BottomEnd),
    ) {
        NewVersionPopupCard(
            version = version.name,
            changes = version.changelogs.firstOrNull()?.changes?.lineSequence()?.toList().orEmpty(),
            onDetailsClick = { uriHandler.openUri("https://github.com/open-ani/animeko/releases/tag/${version.name}") },
            onAutoUpdateClick = onAutoUpdateClick,
            onDismissRequest = onDismiss,
        )
    }
}

// ───────────────────────── Mobile implementation ─────────────────────────────
@Composable
private fun MobileSnackbar(
    hostState: SnackbarHostState,
    version: NewVersion,
    dismissedManually: Boolean,
    onAutoUpdateClick: () -> Unit,
    onDismissChanged: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    /*
     * Show the snackbar **at most once** for this `version` within this session.
     * We leverage `LaunchedEffect(version.name)` to achieve that.
     */
    LaunchedEffect(version.name) {
        val result = hostState.showSnackbar(
            message = "发现新版本 ${version.name}",
            actionLabel = "查看",
            withDismissAction = false,
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            androidx.compose.material3.SnackbarResult.ActionPerformed -> onDismissChanged(false)
            else -> onDismissChanged(true) // user swiped it away / dismissed
        }
    }

    // If snackbar action is clicked, we pop up a dialog containing the card.
    if (!dismissedManually) {
        androidx.compose.material3.BasicAlertDialog(
            onDismissRequest = { onDismissChanged(true) },
        ) {
            NewVersionPopupCard(
                version = version.name,
                changes = version.changelogs.firstOrNull()?.changes?.lineSequence()?.toList().orEmpty(),
                onDetailsClick = { uriHandler.openUri("https://github.com/open-ani/animeko/releases/tag/${version.name}") },
                onAutoUpdateClick = onAutoUpdateClick,
                onDismissRequest = { onDismissChanged(true) },
            )
        }
    }
}
