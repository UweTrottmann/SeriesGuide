package com.battlelancer.seriesguide.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.CloudSetupActivity
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.ActivityMoreOptionsBinding
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.sync.SyncProgress
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.util.PackageTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.WebTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnClick
import com.battlelancer.seriesguide.util.safeShow
import com.uwetrottmann.androidutils.AndroidUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale


/**
 * Displays accounts, links to unlock all features, settings and help
 * and if the app does no longer receive updates.
 */
class MoreOptionsActivity : BaseTopActivity() {

    private lateinit var binding: ActivityMoreOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreOptionsBinding.inflate(layoutInflater)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        setContentView(binding.root)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_more)

        configureViews()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun configureViews() {
        // Shows a no updates info text if the device is running a version of Android
        // that will not be supported by a future version of this app.
        binding.textViewNoMoreUpdates.isGone = AndroidUtils.isLollipopOrHigher

        binding.syncStatus.isGone = true

        binding.containerCloud.setOnClickListener {
            startActivity(Intent(this, CloudSetupActivity::class.java))
        }
        binding.containerTrakt.setOnClickListener {
            startActivity(Intent(this, ConnectTraktActivity::class.java))
        }
        binding.buttonSupportTheApp.setOnClickListener {
            startActivity(Utils.getBillingActivityIntent(this))
        }
        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SeriesGuidePreferences::class.java))
        }
        binding.buttonHelp.setOnClickListener {
            WebTools.openInCustomTab(this, getString(R.string.help_url))
        }
        ViewTools.openUriOnClick(binding.buttonCommunity, getString(R.string.url_community))
        binding.buttonFeedback.setOnClickListener {
            startActivity(getFeedbackEmailIntent(this))
        }
        ViewTools.openUriOnClick(binding.buttonTranslations, getString(R.string.url_translations))
        ViewTools.openUriOnClick(
            binding.buttonContributeContent,
            getString(R.string.url_contribute_content)
        )
        binding.buttonDebugView.setOnClickListener {
            if (AppSettings.isUserDebugModeEnabled(this)) {
                DebugViewFragment().safeShow(supportFragmentManager, "debugViewDialog")
            }
        }
        binding.buttonMoreAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        binding.textViewMoreVersionInfo.text = PackageTools.getVersionString(this)
        binding.textViewMoreVersionInfo.copyTextToClipboardOnClick()
    }

    override fun onStart() {
        super.onStart()

        // Update accounts.
        binding.textViewCloudAccount.apply {
            if (HexagonSettings.isEnabled(this@MoreOptionsActivity)) {
                text = HexagonSettings.getAccountName(this@MoreOptionsActivity)
            } else {
                setText(R.string.hexagon_signin)
            }
        }
        binding.textViewTraktAccount.apply {
            if (TraktCredentials.get(this@MoreOptionsActivity).hasCredentials()) {
                text = TraktCredentials.get(this@MoreOptionsActivity).username
            } else {
                setText(R.string.connect_trakt)
            }
        }

        // Update supporter status.
        binding.textViewThankYouSupporters.isGone = !Utils.hasAccessToX(this)

        // Show debug view button if debug mode is on.
        binding.buttonDebugView.isGone = !AppSettings.isUserDebugModeEnabled(this)
    }


    override val snackbarParentView: View
        get() = binding.coordinatorLayoutMoreOptions

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncProgress.SyncEvent) {
        binding.syncStatus.setProgress(event)
    }



    companion object {
        private const val SUPPORT_MAIL = "support@seriesgui.de"

        @JvmStatic
        fun getFeedbackEmailIntent(context: Context): Intent {
            val intent = Intent(Intent.ACTION_SENDTO)
                .setData(Uri.parse("mailto:"))
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_MAIL))
                // include app version in subject
                .putExtra(
                    Intent.EXTRA_SUBJECT,
                    "SeriesGuide ${PackageTools.getVersion(context)} Feedback"
                )
                // and hardware and Android info in body
                .putExtra(
                    Intent.EXTRA_TEXT,
                    "${Build.MANUFACTURER.uppercase(Locale.US)} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}\n\n"
                )
            return Intent.createChooser(intent, context.getString(R.string.feedback))
        }
    }

}
