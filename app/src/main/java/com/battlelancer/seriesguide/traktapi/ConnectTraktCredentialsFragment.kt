package com.battlelancer.seriesguide.traktapi

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.FragmentConnectTraktCredentialsBinding
import com.battlelancer.seriesguide.sync.SyncProgress.SyncEvent
import com.battlelancer.seriesguide.ui.SearchActivity.Companion.newIntent
import com.battlelancer.seriesguide.util.ThemeUtils
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

        // make learn more link clickable
        binding.textViewTraktAbout.movementMethod = LinkMovementMethod.getInstance()

        val hexagonEnabled = HexagonSettings.isEnabled(requireContext())
        binding.featureStatusTraktCheckIn.setFeatureEnabled(!hexagonEnabled)
        binding.featureStatusTraktSync.setFeatureEnabled(!hexagonEnabled)
        binding.featureStatusTraktSyncShows.setFeatureEnabled(!hexagonEnabled)
        binding.featureStatusTraktSyncMovies.setFeatureEnabled(!hexagonEnabled)

        // library button
        binding.buttonTraktLibrary.setOnClickListener {
            // open search tab, will now have links to trakt lists
            startActivity(newIntent(requireContext()))
        }

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
            setAccountButtonState(false)
            binding.buttonTraktLibrary.visibility = View.VISIBLE
        } else {
            binding.textViewTraktUser.text = null
            setAccountButtonState(true)
            binding.buttonTraktLibrary.visibility = View.GONE
        }
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