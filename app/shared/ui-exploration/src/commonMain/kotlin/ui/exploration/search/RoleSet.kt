/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package me.him188.ani.app.ui.exploration.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.him188.ani.app.data.models.subject.PersonPosition
import me.him188.ani.app.data.models.subject.RelatedPersonInfo
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
value class RoleSet(
    private val delegate: List<Role>,
) {
    operator fun plus(other: RoleSet): RoleSet = RoleSet(delegate + other.delegate)
    operator fun minus(other: RoleSet): RoleSet = RoleSet(delegate - other.delegate.toSet())
    operator fun contains(role: Role): Boolean = role in delegate

    @Stable
    companion object {
        @Stable
        val Empty = RoleSet(emptyList())

        @Stable
        val Default =
            RoleSet(listOf(Role.ANIMATION_PRODUCTION, Role.DIRECTOR, Role.SCRIPT_WRITER, Role.MUSIC))
    }
}

/**
 * 过滤 [RelatedPersonInfo] 中的角色, 返回符合条件的 [RelatedPersonInfo]
 */
fun List<RelatedPersonInfo>.filter(roleSet: RoleSet): Sequence<RelatedPersonInfo> {
    return asSequence().filter f@{ person ->
        person.position in roleSet
    }
}

typealias Role = PersonPosition
