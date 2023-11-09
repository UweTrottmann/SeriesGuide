// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui

import com.battlelancer.seriesguide.movies.MoviesActivityImpl

/**
 * Shell to avoid breaking AndroidManifest.xml and shortcuts.xml and other references to this name.
 * Implementation moved to feature-specific package.
 */
class MoviesActivity : MoviesActivityImpl()
