// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * A [androidx.preference.PreferenceFragmentCompat] which configures the RecyclerView displaying the
 * settings to adjust bottom padding for the navigation bar.
 */
abstract class BasePreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        // Add bottom padding to make it obvious where the list ends and to avoid accidental taps
        // on the navigation bar.
        val bottomPadding = resources.getDimensionPixelSize(R.dimen.large_padding)
        recyclerView.updatePadding(bottom = bottomPadding)
        // Adjust preferences RecyclerView bottom padding to navigation bar height.
        ThemeUtils.applyBottomPaddingForNavigationBar(recyclerView)
        return recyclerView
    }

}
