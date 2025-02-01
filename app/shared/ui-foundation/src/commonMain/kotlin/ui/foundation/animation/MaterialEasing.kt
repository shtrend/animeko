/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.foundation.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

// https://m3.material.io/styles/motion/easing-and-duration/tokens-specs#433b1153-2ea3-4fe2-9748-803a47bc97ee

// md.sys.motion.easing.standard
val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)

// md.sys.motion.easing.standard.decelerate
val StandardDecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1f)

// md.sys.motion.easing.standard.accelerate
val StandardAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 1f, 1f)

// TODO: EmphasizedEasing should actually be pathInterpolator(M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1)
// md.sys.motion.easing.emphasized
val EmphasizedEasing: Easing = StandardEasing

// md.sys.motion.easing.emphasized.decelerate
val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

// md.sys.motion.easing.emphasized.accelerate
val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
