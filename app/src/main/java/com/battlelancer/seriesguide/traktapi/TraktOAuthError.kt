// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.traktapi

/**
 * Error for tracking OAuth failures.
 */
internal class TraktOAuthError(action: String, failure: String) : Throwable("$action: $failure")