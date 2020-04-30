package com.battlelancer.seriesguide.ui.preferences

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_SYSTEM
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.CloudSetupActivity
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.ActivityMoreOptionsBinding
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.DebugViewFragment
import com.battlelancer.seriesguide.ui.HelpActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
import com.uwetrottmann.seriesguide.customtabs.CustomTabsHelper


/**
 * Displays accounts, links to unlock all features, settings and help
 * and if the app does no longer receive updates.
 */
class MoreOptionsActivity : BaseTopActivity() {

    private lateinit var binding: ActivityMoreOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_more)

        configureViews()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun configureViews() {
        // Shows a no updates info text if the device is running a version of Android
        // that will not be supported by a future version of this app.
        binding.textViewNoMoreUpdates.isGone = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

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
            // If we cant find a package name, it means there is no browser that supports
            // Chrome Custom Tabs installed. So, we fallback to the WebView activity.
            val packageName: String? = CustomTabsHelper.getPackageNameToUse(this)
            val intent = if (packageName == null) {
                Intent(this, HelpActivity::class.java)
            } else {
                val darkParams = CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(
                        ContextCompat.getColor(this, R.color.sg_background_app_bar_dark)
                    )
                    .build()
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(
                        ContextCompat.getColor(this, R.color.sg_color_primary_light)
                    )
                    .setColorScheme(COLOR_SCHEME_SYSTEM)
                    .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
                    .build().intent.apply {
                        setPackage(packageName)
                        data = Uri.parse(getString(R.string.help_url))
                    }
            }
            startActivity(intent)
        }
        binding.buttonCommunity.setOnClickListener {
            Utils.launchWebsite(this, getString(R.string.url_community))
        }
        binding.buttonFeedback.setOnClickListener {
            startActivity(HelpActivity.getFeedbackEmailIntent(this))
        }
        binding.buttonTranslations.setOnClickListener {
            Utils.launchWebsite(this, getString(R.string.url_translations))
        }
        binding.buttonDebugView.setOnClickListener {
            if (AppSettings.isUserDebugModeEnabled(this)) {
                DebugViewFragment().safeShow(supportFragmentManager, "debugViewDialog")
            }
        }
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

    override fun getSnackbarParentView(): View {
        return binding.coordinatorLayoutMoreOptions
    }
}
