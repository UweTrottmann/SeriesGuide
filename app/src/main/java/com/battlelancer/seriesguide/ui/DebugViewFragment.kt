package com.battlelancer.seriesguide.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktOAuthSettings
import io.palaima.debugdrawer.actions.ActionsModule
import io.palaima.debugdrawer.actions.ButtonAction
import io.palaima.debugdrawer.commons.DeviceModule
import io.palaima.debugdrawer.timber.TimberModule
import io.palaima.debugdrawer.view.DebugView

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

        val buttonClearTraktRefreshToken = ButtonAction("Clear trakt refresh token") {
            TraktOAuthSettings.storeRefreshData(requireContext(), "", 3600 /* 1 hour */)
        }

        val buttonInvalidateTraktAccessToken = ButtonAction("Invalidate trakt access token") {
            TraktCredentials.get(requireContext()).storeAccessToken("invalid-token")
        }

        val buttonInvalidateTraktRefreshToken = ButtonAction("Invalidate trakt refresh token") {
            TraktOAuthSettings.storeRefreshData(requireContext(), "invalid-token", 3600 /* 1 hour */)
        }

        val buttonTriggerJobProcessor = ButtonAction("Schedule job processing") {
            SgSyncAdapter.requestSyncJobsImmediate(requireContext())
        }

        debugView.modules(
            ActionsModule(
                buttonClearTraktRefreshToken,
                buttonInvalidateTraktAccessToken,
                buttonInvalidateTraktRefreshToken,
                buttonTriggerJobProcessor
            ),
            TimberModule("${requireContext().packageName}.fileprovider"),
            DeviceModule()
        )

        return view
    }

}
