// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: GPL-3.0-or-later

package com.battlelancer.seriesguide

import android.app.Application

/**
 * Empty [Application] to use with Robolectric @Config to avoid errors
 * with components that require an actual device to initialize.
 */
class EmptyTestApplication : Application()