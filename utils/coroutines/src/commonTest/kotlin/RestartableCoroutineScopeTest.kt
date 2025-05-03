/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.utils.coroutines

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class RestartableCoroutineScopeTest {

    private val testScope = TestScope()
    private lateinit var scope: RestartableCoroutineScope

    @BeforeTest
    fun setUp() {
        scope = RestartableCoroutineScope(testScope.coroutineContext)
    }

    @Test
    fun `launch starts coroutine`() = testScope.runTest {
        val result = CompletableDeferred<Int>()

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            result.complete(42)
        }

        assertEquals(42, result.await())
        scope.close()
    }

    @Test
    fun `cancel stops running coroutines`() = testScope.runTest {
        val isRunning = CompletableDeferred<Boolean>()

        val job = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) {
            try {
                awaitCancellation()
            } catch (e: Exception) {
                isRunning.complete(false)
            }
        }

        // Ensure the job has started
        delay(10)
        assertTrue(job.isActive)

        scope.restart()
        assertFalse(job.isActive)
        assertFalse(isRunning.await())
        scope.close()
    }

    @Test
    fun `can launch new coroutines after cancel`() = testScope.runTest {
        // Start and cancel initial coroutines
        val job1 = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) { delay(1000) }
        scope.restart()
        assertFalse(job1.isActive)

        // Launch a new coroutine after cancellation
        val result = CompletableDeferred<Int>()
        val job2 = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) {
            result.complete(42)
        }

        // The new coroutine should run successfully
        assertEquals(42, result.await())
        assertNotEquals(job1, job2)
        scope.close()
    }

    @Test
    fun `close cancels all coroutines and allows new launches`() = testScope.runTest {
        val job1 = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) { delay(1000) }

        scope.close()
        assertFalse(job1.isActive)

        // Attempt to launch after close should return a completed job
        val job2 = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) { delay(1000) }
        assertFalse(job2.isActive)
        scope.close()
    }

    @Test
    fun `restart doesnt affect parent scope`() = testScope.runTest {
        val parentJob = coroutineContext.job
        val initialActiveState = parentJob.isActive

        scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) { delay(1000) }
        scope.restart()

        assertEquals(initialActiveState, parentJob.isActive)
        scope.close()
    }

    @Test
    fun `multiple launches work independently`() = testScope.runTest {
        val results = List(5) { CompletableDeferred<Int>() }

        results.forEachIndexed { index, deferred ->
            scope.launch {
                delay(index * 100L)
                deferred.complete(index)
            }
        }

        results.forEachIndexed { index, deferred ->
            assertEquals(index, deferred.await())
        }
        scope.close()
    }

    @Test
    fun `cancel affects only current child jobs`() = testScope.runTest {
        val beforeCancelResult = CompletableDeferred<Int>()
        val afterCancelResult = CompletableDeferred<Int>()

        scope.launch {
            delay(100)
            beforeCancelResult.complete(1)
        }

        // Cancel before the first job completes
        scope.restart()

        // Launch a new job after cancellation
        scope.launch {
            afterCancelResult.complete(2)
        }

        // First job should be cancelled
        advanceTimeBy(200)
        assertFalse(beforeCancelResult.isCompleted)

        // Second job should complete
        assertEquals(2, afterCancelResult.await())
        scope.close()
    }

    @Test
    fun `thread-safety - concurrent launches`() = testScope.runTest {
        val completedJobs = CompletableDeferred<Int>()
        var completed = 0
        val lock = SynchronizedObject()

        val jobs = List(100) {
            async {
                val job = scope.launch(
                    start = CoroutineStart.UNDISPATCHED,
                ) {
                    delay(10)
                    kotlinx.atomicfu.locks.synchronized(lock) {
                        completed++
                        if (completed == 100) {
                            completedJobs.complete(completed)
                        }
                    }
                }
                job
            }
        }

        jobs.forEach { it.await() }
        assertEquals(100, completedJobs.await())
        scope.close()
    }

    @Test
    fun `thread-safety - concurrent cancellations`() = testScope.runTest {
        // Create tasks that will run if not cancelled
        val results = List(50) { CompletableDeferred<Boolean>() }

        results.forEachIndexed { index, deferred ->
            scope.launch(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                delay(100.milliseconds)
                deferred.complete(true)
            }
        }

        // Launch multiple cancellations and new jobs concurrently
        val cancellationJobs = List(10) {
            async {
                scope.restart()
                scope.launch(
                    start = CoroutineStart.UNDISPATCHED,
                ) {
                    // These should run
                    delay(10.milliseconds)
                }
            }
        }

        cancellationJobs.forEach { it.await() }

        // Original jobs should be cancelled
        delay(200.milliseconds)
        results.forEach { deferred ->
            assertFalse(deferred.isCompleted)
        }
        scope.close()
    }

    @Test
    fun `child job cancellation doesnt affect other children`() = testScope.runTest {
        val result2 = CompletableDeferred<Int>()

        val job2Run = CompletableDeferred<Unit>()

        val job1 = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) {
            awaitCancellation()
        }

        val job2 = scope.launch(
            start = CoroutineStart.UNDISPATCHED,
        ) {
            job2Run.await()
            result2.complete(2)
        }

        // Cancel just one job
        job1.cancel()

        job2Run.complete(Unit)
        runCurrent()

        assertEquals(2, result2.await())
        scope.close()
    }

    @Test
    fun `cancelled scope doesnt leak memory`() = testScope.runTest {
        val deferreds = mutableListOf<CompletableDeferred<Unit>>()

        // Launch coroutines that would run forever
        repeat(100) {
            val deferred = CompletableDeferred<Unit>()
            deferreds.add(deferred)

            @OptIn(DelicateCoroutinesApi::class)
            scope.launch(start = CoroutineStart.ATOMIC) {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    deferred.complete(Unit)
                }
            }
        }

        scope.closeAndJoin()

        // All coroutines should have their finally blocks executed
        deferreds.forEach { deferred ->
            assertTrue(deferred.isCompleted)
        }
    }
}