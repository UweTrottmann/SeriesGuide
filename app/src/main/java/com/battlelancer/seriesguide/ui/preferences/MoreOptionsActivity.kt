package com.battlelancer.seriesguide.ui.preferences

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.CloudSetupActivity
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.ActivityMoreOptionsBinding
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.HelpActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.util.Utils
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

    private fun configureViews() {
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
                val builder = CustomTabsIntent.Builder()
                builder.setShowTitle(true)
                builder.setToolbarColor(
                    ContextCompat.getColor(
                        this,
                        Utils.resolveAttributeToResourceId(theme, R.attr.colorPrimary)
                    )
                )

                builder.build().intent.apply {
                    setPackage(packageName)
                    data = Uri.parse(getString(R.string.help_url))
                }
            }
            startActivity(intent)
        }
        binding.buttonFeedback.setOnClickListener {
            startActivity(HelpActivity.getFeedbackEmailIntent(this))
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
    }

    override fun getSnackbarParentView(): View {
        return binding.coordinatorLayoutMoreOptions
    }
}
