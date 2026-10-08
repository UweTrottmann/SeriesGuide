// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class SharedBlockingTaskTest {

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
    }

    /**
     * Creates a task whose block counts its runs, waits until [release] is called and
     * returns the number of the run (starting with 1). If [throwOnFirstRun] is set, the first run
     * throws [exception] instead.
     */
    private class TaskTester(throwOnFirstRun: Boolean = false) {
        private val started = CountDownLatch(1)
        private val release = CountDownLatch(1)
        val runCount = AtomicInteger()
        val exception = IllegalStateException("expected")

        val task = SharedBlockingTask("TestTask") {
            val run = runCount.incrementAndGet()
            started.countDown()
            assertThat(release.await(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
            if (throwOnFirstRun && run == 1) throw exception
            run
        }

        /**
         * Lets the block of the task finish.
         */
        fun release() {
            release.countDown()
        }

        /**
         * Waits until the block of the task has started running.
         */
        fun awaitStarted() {
            assertThat(started.await(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    /**
     * Calls [SharedBlockingTask.runOrAwait] on its own thread and records what happened.
     */
    private class TaskCaller(task: SharedBlockingTask<Int>) {
        val result = AtomicReference<Int?>()
        val error = AtomicReference<Throwable?>()
        val interruptedAfterReturn = AtomicReference<Boolean?>()

        val thread = thread(name = "TestCaller") {
            try {
                result.set(task.runOrAwait())
            } catch (e: Throwable) {
                error.set(e)
            }
            interruptedAfterReturn.set(Thread.currentThread().isInterrupted)
        }

        /**
         * Polls until this caller waits for the result of a run.
         */
        fun awaitWaiting() {
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DEFAULT_TIMEOUT_MS)
            while (thread.state != Thread.State.WAITING) {
                if (System.nanoTime() > deadline) {
                    throw AssertionError("Caller did not start waiting, state: ${thread.state}")
                }
                Thread.sleep(1)
            }
        }

        fun awaitFinished() {
            thread.join(DEFAULT_TIMEOUT_MS)
            assertThat(thread.isAlive).isFalse()
        }
    }

    @Test(timeout = 20000)
    fun singleCaller_returnsResult() {
        val tester = TaskTester()
        tester.release()

        assertThat(tester.task.runOrAwait()).isEqualTo(1)
    }

    @Test(timeout = 20000)
    fun concurrentCallers_shareOneRun() {
        val tester = TaskTester()

        val first = TaskCaller(tester.task)
        tester.awaitStarted()
        val second = TaskCaller(tester.task)
        second.awaitWaiting()

        tester.release()
        first.awaitFinished()
        second.awaitFinished()

        assertThat(first.result.get()).isEqualTo(1)
        assertThat(second.result.get()).isEqualTo(1)
        assertThat(tester.runCount.get()).isEqualTo(1)
    }

    @Test(timeout = 20000)
    fun interruptedCallers_stillGetResultAndKeepInterruptedState() {
        val tester = TaskTester()

        val first = TaskCaller(tester.task)
        tester.awaitStarted()
        val second = TaskCaller(tester.task)
        first.awaitWaiting()
        second.awaitWaiting()

        // Interrupting does not end the task and the result is delivered.
        // Interrupting one caller does not affect the other caller.
        first.thread.interrupt()
        tester.release()
        first.awaitFinished()
        second.awaitFinished()

        assertThat(first.error.get()).isNull()
        assertThat(first.result.get()).isEqualTo(1)
        assertThat(second.result.get()).isEqualTo(1)
        assertThat(first.interruptedAfterReturn.get()).isTrue()
        assertThat(second.interruptedAfterReturn.get()).isFalse()
        assertThat(tester.runCount.get()).isEqualTo(1)
    }

    @Test(timeout = 20000)
    fun alreadyInterrupted_stillRunsAndKeepsInterruptedState() {
        val tester = TaskTester()
        tester.release()

        Thread.currentThread().interrupt()
        val result = tester.task.runOrAwait()

        assertThat(result).isEqualTo(1)
        assertThat(Thread.currentThread().isInterrupted).isTrue()

        // Clear to not affect other tests
        Thread.interrupted()
    }

    @Test(timeout = 20000)
    fun callAfterCompletion_startsNewRun() {
        val tester = TaskTester()
        tester.release()

        val first = tester.task.runOrAwait()
        val second = tester.task.runOrAwait()

        assertThat(first).isEqualTo(1)
        assertThat(second).isEqualTo(2)
        assertThat(tester.runCount.get()).isEqualTo(2)
    }

    @Test(timeout = 20000)
    fun blockThrows_allCallersGetExecutionException() {
        val tester = TaskTester(throwOnFirstRun = true)

        val first = TaskCaller(tester.task)
        tester.awaitStarted()
        val second = TaskCaller(tester.task)
        first.awaitWaiting()
        second.awaitWaiting()

        tester.release()
        first.awaitFinished()
        second.awaitFinished()

        for (caller in listOf(first, second)) {
            assertThat(caller.result.get()).isNull()
            assertThat(caller.error.get()).isInstanceOf(ExecutionException::class.java)
            assertThat(caller.error.get()?.cause).isSameInstanceAs(tester.exception)
        }
        assertThat(tester.runCount.get()).isEqualTo(1)

        // A later call starts a fresh run, which succeeds
        assertThat(tester.task.runOrAwait()).isEqualTo(2)
    }
}
