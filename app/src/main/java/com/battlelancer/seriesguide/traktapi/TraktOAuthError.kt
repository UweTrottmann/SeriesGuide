// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: GPL-3.0-or-later

package com.battlelancer.seriesguide.traktapi

/**
 * Error for tracking OAuth failures.
 */
internal class TraktOAuthError(action: String, failure: String) : Throwable("$action: $failure")