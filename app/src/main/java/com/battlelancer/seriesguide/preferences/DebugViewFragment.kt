// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.preferences


import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.databinding.FragmentDebugViewBinding
import com.battlelancer.seriesguide.diagnostics.DebugLogActivity
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2WithShow
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktOAuthSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Displays debug actions. Notably allows to display and share logs.
 */
class DebugViewFragment : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentDebugViewBinding.inflate(layoutInflater)

        binding.buttonDebugViewDisplayLogs.setOnClickListener {
            startActivity(DebugLogActivity.intent(requireContext()))
        }

        binding.buttonDebugViewTestNotification1.setOnClickListener {
            showTestNotification(1)
        }

        binding.buttonDebugViewTestNotification3.setOnClickListener {
            showTestNotification(3)
        }

        binding.buttonDebugViewTraktClearRefreshToken.setOnClickListener {
            TraktOAuthSettings.storeRefreshData(requireContext(), "", 3600 /* 1 hour */)
        }

        binding.buttonDebugViewTraktInvalidateAccessToken.setOnClickListener {
            TraktCredentials.get(requireContext()).storeAccessToken("invalid-token")
        }

        binding.buttonDebugViewTraktInvalidateRefreshToken.setOnClickListener {
            TraktOAuthSettings.storeRefreshData(
                requireContext(),
                "invalid-token",
                3600 /* 1 hour */
            )
        }

        binding.buttonDebugViewTriggerJobProcessing.setOnClickListener {
            SgSyncAdapter.requestSyncJobsImmediate(requireContext())
        }

        binding.buttonDebugViewDemoMode.setOnClickListener {
            toggleDemoMode()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.getRoot())
            .create()
    }

    private fun showTestNotification(episodeCount: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            // To use different episodes for one vs. multiple just use OFFSET
            val query = ("${SgEpisode2WithShow.SELECT} LIMIT $episodeCount OFFSET $episodeCount")
            val episodes = SgRoomDatabase.getInstance(requireContext()).sgEpisode2Helper()
                .getEpisodesWithShow(SimpleSQLiteQuery(query, null))
            NotificationService(requireContext()).notifyAbout(
                episodes,
                episodes.mapIndexed { index, _ -> index }, // first one
                0 // not stored
            )
        }
    }

    private fun toggleDemoMode() {
        val isEnabledOld = AppSettings.isDemoModeEnabled(requireContext())
        AppSettings.setDemoModeState(requireContext(), !isEnabledOld)
        val isEnabledNew = AppSettings.isDemoModeEnabled(requireContext())
        Toast.makeText(requireContext(), "Demo mode: $isEnabledNew", Toast.LENGTH_LONG).show()
    }

}
