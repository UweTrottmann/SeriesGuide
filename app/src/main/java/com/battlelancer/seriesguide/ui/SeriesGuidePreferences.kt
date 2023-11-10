// Copyright 2011-2023 Uwe Trottmann
// Copyright 2011 Jake Wharton
// Copyright 2011 dqdb
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui

import androidx.annotation.StyleRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.preferences.PreferencesActivityImpl

/**
 * Shell to avoid breaking AndroidManifest.xml and shortcuts.xml and other references to this name.
 * Implementation moved to feature-specific package.
 */
class SeriesGuidePreferences : PreferencesActivityImpl() {

    companion object {

        @StyleRes
        @JvmField
        var THEME = R.style.Theme_SeriesGuide_DayNight
    }

}
