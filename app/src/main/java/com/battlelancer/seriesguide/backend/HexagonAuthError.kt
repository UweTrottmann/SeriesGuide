// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.backend

/**
 * Error for tracking Cloud Sign-In failures.
 */
class HexagonAuthError(
    val action: String,
    failure: String,
    cause: Throwable?
) : Throwable("$action: $failure", cause) {

    companion object {

        /**
         * Uses the given action and message of the throwable to build a [HexagonAuthError].
         */
        fun build(action: String, throwable: Throwable): HexagonAuthError {
            return HexagonAuthError(
                action,
                throwable.message ?: "",
                throwable
            )
        }
    }

}
