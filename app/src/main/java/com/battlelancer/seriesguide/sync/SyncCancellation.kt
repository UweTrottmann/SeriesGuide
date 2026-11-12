// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

/**
 * Signals that a sync should stop as soon as possible. Unlike the thread interrupted state,
 * this can not be cleared by other code. Implemented by the host running the sync
 * (currently [SgSyncAdapter]).
 */
fun interface SyncCancellation {

    fun isCanceled(): Boolean

    companion object {
        /** A [SyncCancellation] that is never canceled. */
        val NEVER = SyncCancellation { false }
    }
}

/**
 * Thrown by [throwIfCanceled] if the sync was canceled.
 */
class SyncCanceledException : Exception("Sync canceled")

/**
 * Throws [SyncCanceledException] if [SyncCancellation.isCanceled].
 */
@Throws(SyncCanceledException::class)
fun SyncCancellation.throwIfCanceled() {
    if (isCanceled()) throw SyncCanceledException()
}
