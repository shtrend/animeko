/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.FontLoadResult

val LocalPlatformFontFamily = staticCompositionLocalOf<PlatformFontFamily> {
    error("No PlatformFontFamily provided")
}

class PlatformFontFamily(
    /**
     * `null` for default.
     */
    val defaultFontFamily: FontFamily?
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun rememberPlatformFontFamily(
    fontName: String?,
): PlatformFontFamily {
    if (fontName == null) return PlatformFontFamily(null)

    var resolvedFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    val fontFamilyResolver = LocalFontFamilyResolver.current

    LaunchedEffect(fontFamilyResolver) {
        val fontFamily = FontFamily(fontName)
        resolvedFontFamily = runCatching {
            val result = fontFamilyResolver.resolve(fontFamily).value as FontLoadResult
            if (result.typeface == null || result.typeface?.familyName != fontName) {
                null
            } else {
                fontFamily
            }
        }.getOrNull()
    }

    return PlatformFontFamily(resolvedFontFamily)
}

@Composable
fun Typography.copyWithPlatformFontFamily(
    platformFontFamily: PlatformFontFamily,
): Typography {
    return copy(
        bodyLarge = bodyLarge.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: bodyLarge.fontFamily,
        ),
        bodyMedium = bodyMedium.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: bodyMedium.fontFamily,
        ),
        bodySmall = bodySmall.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: bodySmall.fontFamily,
        ),
        titleLarge = titleLarge.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: titleLarge.fontFamily,
        ),
        titleMedium = titleMedium.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: titleMedium.fontFamily,
        ),
        titleSmall = titleSmall.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: titleSmall.fontFamily,
        ),
        labelLarge = labelLarge.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: labelLarge.fontFamily,
        ),
        labelMedium = labelMedium.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: labelMedium.fontFamily,
        ),
        labelSmall = labelSmall.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: labelSmall.fontFamily,
        ),
        displayLarge = displayLarge.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: displayLarge.fontFamily,
        ),
        displayMedium = displayMedium.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: displayMedium.fontFamily,
        ),
        displaySmall = displaySmall.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: displaySmall.fontFamily,
        ),
        headlineLarge = headlineLarge.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: headlineLarge.fontFamily,
        ),
        headlineMedium = headlineMedium.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: headlineMedium.fontFamily,
        ),
        headlineSmall = headlineSmall.copy(
            fontFamily = platformFontFamily.defaultFontFamily ?: headlineSmall.fontFamily,
        ),
    )
}