/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.utils.platform.collections

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

class ConcurrentQueue<E> {
    private val delegate = ArrayDeque<E>()
    private val lock = ReentrantLock()

    fun add(element: E): Boolean {
        lock.withLock {
            return delegate.add(element)
        }
    }

    fun remove(element: E): Boolean {
        lock.withLock {
            return delegate.remove(element)
        }
    }

    fun removeFirst(): E {
        lock.withLock {
            return delegate.removeAt(0)
        }
    }

    fun removeFirstOrNull(): E? {
        lock.withLock {
            return delegate.removeFirstOrNull()
        }
    }

    fun firstOrNull(): E? {
        lock.withLock {
            return delegate.firstOrNull()
        }
    }

    fun addLast(element: E) {
        lock.withLock {
            return delegate.addLast(element)
        }
    }

    fun isEmpty(): Boolean {
        lock.withLock {
            return delegate.isEmpty()
        }
    }

    fun isNotEmpty(): Boolean {
        lock.withLock {
            return delegate.isNotEmpty()
        }
    }
}