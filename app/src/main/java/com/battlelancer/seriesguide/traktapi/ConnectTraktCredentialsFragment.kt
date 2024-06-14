// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.FragmentConnectTraktCredentialsBinding
import com.battlelancer.seriesguide.shows.ShowsActivityImpl
import com.battlelancer.seriesguide.sync.SyncProgress.SyncEvent
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Interface connect or disconnect Trakt, also shows features not supported while Cloud is signed in.
 */
class ConnectTraktCredentialsFragment : Fragment() {

    private var binding: FragmentConnectTraktCredentialsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentConnectTraktCredentialsBinding.inflate(inflater, container, false)
            .also { binding = it }

        ThemeUtils.applyBottomPaddingForNavigationBar(binding.scrollViewTraktCredentials)

        val hexagonEnabled = HexagonSettings.isEnabled(requireContext())
        binding.featureStatusTraktCheckIn.setFeatureEnabled(!hexagonEnabled)
        binding.featureStatusTraktSync.setFeatureEnabled(!hexagonEnabled)
        binding.featureStatusTraktSyncShows.setFeatureEnabled(!hexagonEnabled)
        binding.featureStatusTraktSyncMovies.setFeatureEnabled(!hexagonEnabled)

        // library button
        binding.buttonTraktLibrary.setOnClickListener {
            // Show discover tab, will now have links to trakt lists
            startActivity(
                ShowsActivity.newIntent(requireContext(), ShowsActivityImpl.Tab.DISCOVER.index)
            )
        }
        // Learn more button
        ViewTools.openUrlOnClickAndCopyOnLongPress(
            binding.buttonTraktWebsite,
            getString(R.string.url_trakt)
        )
        // VIP button
        ViewTools.openUrlOnClickAndCopyOnLongPress(
            binding.buttonTraktSupport,
            getString(R.string.url_trakt_vip)
        )
        ViewTools.openUrlOnClickAndCopyOnLongPress(
            binding.buttonTraktDeleteAccount,
            getString(R.string.url_trakt_delete_account)
        )

        binding.syncStatusTrakt.visibility = View.GONE

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        updateViews()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncEvent?) {
        binding?.syncStatusTrakt?.setProgress(event)
    }

    private fun updateViews() {
        val binding = binding ?: return
        val traktCredentials = TraktCredentials.get(requireContext())
        val hasCredentials = traktCredentials.hasCredentials()
        if (hasCredentials) {
            var username = traktCredentials.username
            val displayName = traktCredentials.displayName
            if (!displayName.isNullOrEmpty()) {
                username += " ($displayName)"
            }
            binding.textViewTraktUser.text = username
        } else {
            binding.textViewTraktUser.text = null
        }
        setAccountButtonState(!hasCredentials)
        binding.buttonTraktLibrary.isGone = !hasCredentials
        binding.buttonTraktSupport.isGone = !hasCredentials
    }

    private fun connect() {
        val binding = binding ?: return
        binding.buttonTraktConnect.isEnabled = false
        startActivity(Intent(activity, TraktAuthActivity::class.java))
    }

    private fun disconnect() {
        TraktCredentials.get(requireContext()).removeCredentials()
        updateViews()
    }

    private fun setAccountButtonState(connectEnabled: Boolean) {
        val buttonAccount = binding?.buttonTraktConnect ?: return
        buttonAccount.isEnabled = true
        buttonAccount.setText(if (connectEnabled) R.string.connect else R.string.disconnect)
        if (connectEnabled) {
            buttonAccount.setOnClickListener { connect() }
        } else {
            buttonAccount.setOnClickListener { disconnect() }
        }
    }
}