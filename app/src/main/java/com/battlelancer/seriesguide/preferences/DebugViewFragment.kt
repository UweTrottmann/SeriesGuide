package com.battlelancer.seriesguide.preferences


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2WithShow
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktOAuthSettings
import io.palaima.debugdrawer.actions.ActionsModule
import io.palaima.debugdrawer.actions.ButtonAction
import io.palaima.debugdrawer.commons.DeviceModule
import io.palaima.debugdrawer.timber.TimberModule
import io.palaima.debugdrawer.view.DebugView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Displays a [DebugView]. Notably allows to display and share logs.
 */
class DebugViewFragment : AppCompatDialogFragment() {

    private lateinit var debugView: DebugView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // LogAdapter is hard-coded to white background, so always use a light theme.
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Light)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_debug_view, container, false)

        debugView = view.findViewById(R.id.debugView)
        debugView.background = null // for whatever reason uses windowBackground by default

        val showTestNotification1 = ButtonAction("Show test notification (1)") {
            showTestNotification(1)
        }

        val showTestNotification3 = ButtonAction("Show test notification (3)") {
            showTestNotification(3)
        }

        val buttonClearTraktRefreshToken = ButtonAction("Clear trakt refresh token") {
            TraktOAuthSettings.storeRefreshData(requireContext(), "", 3600 /* 1 hour */)
        }

        val buttonInvalidateTraktAccessToken = ButtonAction("Invalidate trakt access token") {
            TraktCredentials.get(requireContext()).storeAccessToken("invalid-token")
        }

        val buttonInvalidateTraktRefreshToken = ButtonAction("Invalidate trakt refresh token") {
            TraktOAuthSettings.storeRefreshData(
                requireContext(),
                "invalid-token",
                3600 /* 1 hour */
            )
        }

        val buttonTriggerJobProcessor = ButtonAction("Schedule job processing") {
            SgSyncAdapter.requestSyncJobsImmediate(requireContext())
        }

        val buttonDemoMode = ButtonAction("Toggle demo mode") {
            toggleDemoMode()
        }

        debugView.modules(
            ActionsModule(
                "Notifications",
                showTestNotification1,
                showTestNotification3
            ),
            ActionsModule(
                "Trakt",
                buttonClearTraktRefreshToken,
                buttonInvalidateTraktAccessToken,
                buttonInvalidateTraktRefreshToken
            ),
            ActionsModule(
                "Jobs",
                buttonTriggerJobProcessor
            ),
            ActionsModule(
                "Demo mode",
                buttonDemoMode
            ),
            TimberModule("${requireContext().packageName}.fileprovider"),
            DeviceModule()
        )

        return view
    }

    private fun showTestNotification(episodeCount: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val query = ("${SgEpisode2WithShow.SELECT} LIMIT $episodeCount")
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
