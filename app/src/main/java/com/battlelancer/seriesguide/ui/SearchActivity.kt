// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui

import com.battlelancer.seriesguide.shows.search.SearchActivityImpl

/**
 * Shell to avoid breaking AndroidManifest.xml and shortcuts.xml and other references to this name.
 * Implementation moved to feature-specific package.
 */
class SearchActivity : SearchActivityImpl()
