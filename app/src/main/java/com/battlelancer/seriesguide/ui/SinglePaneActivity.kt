// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui

import android.app.Activity
import com.battlelancer.seriesguide.databinding.ActivitySinglepaneBinding
import com.battlelancer.seriesguide.util.ThemeUtils

object SinglePaneActivity {

    fun onCreateFor(activity: Activity): ActivitySinglepaneBinding {
        val binding = ActivitySinglepaneBinding.inflate(activity.layoutInflater)
        activity.setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        return binding
    }

}