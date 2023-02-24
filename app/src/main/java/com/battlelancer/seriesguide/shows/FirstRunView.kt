package com.battlelancer.seriesguide.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewFirstRunBinding
import com.battlelancer.seriesguide.dataliberation.AutoBackupTools
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.settings.UpdateSettings
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ViewTools
import com.uwetrottmann.androidutils.AndroidUtils

class FirstRunView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    interface FirstRunClickListener {
        fun onAddShowClicked()
        fun onSignInClicked()
        fun onRestoreBackupClicked()
        fun onRestoreAutoBackupClicked()
        fun onAllowNotificationsClicked()
        fun onDismissClicked()
    }

    private val binding: ViewFirstRunBinding =
        ViewFirstRunBinding.inflate(LayoutInflater.from(context), this, true)

    var clickListener: FirstRunClickListener? = null

    override fun onFinishInflate() {
        super.onFinishInflate()

        binding.groupAllowNotifications.isGone =
            !AndroidUtils.isAtLeastTiramisu || NotificationSettings.areNotificationsAllowed(context)
        binding.buttonAllowNotifications.setOnClickListener {
            clickListener?.onAllowNotificationsClicked()
        }

        binding.groupAutoBackupDetected.isGone =
            !AutoBackupTools.isAutoBackupMaybeAvailable(context)
        binding.buttonRestoreAutoBackup.setOnClickListener {
            clickListener?.onRestoreAutoBackupClicked()
        }

        binding.containerNoSpoilers.setOnClickListener { v ->
            // new state is inversion of current state
            val noSpoilers = !binding.checkboxNoSpoilers.isChecked
            // save
            PreferenceManager.getDefaultSharedPreferences(v.context).edit {
                putBoolean(DisplaySettings.KEY_PREVENT_SPOILERS, noSpoilers)
            }
            // update next episode strings right away
            TaskManager.getInstance().tryNextEpisodeUpdateTask(v.context)
            // show
            binding.checkboxNoSpoilers.isChecked = noSpoilers
        }
        binding.checkboxNoSpoilers.isChecked = DisplaySettings.preventSpoilers(context)
        binding.checkboxNoSpoilers.text = TextTools.buildTitleAndSummary(
            context,
            R.string.pref_nospoilers,
            R.string.pref_nospoilers_summary
        )

        binding.containerDataSaver.setOnClickListener {
            val isSaveData = !binding.checkboxDataSaver.isChecked
            PreferenceManager.getDefaultSharedPreferences(it.context).edit {
                putBoolean(UpdateSettings.KEY_ONLYWIFI, isSaveData)
            }
            binding.checkboxDataSaver.isChecked = isSaveData
        }
        binding.checkboxDataSaver.isChecked = UpdateSettings.isLargeDataOverWifiOnly(context)
        binding.checkboxDataSaver.text = TextTools.buildTitleAndSummary(
            context,
            R.string.pref_updatewifionly,
            R.string.pref_updatewifionlysummary
        )

        binding.buttonAddShow.setOnClickListener {
            clickListener?.onAddShowClicked()
        }
        binding.buttonSignIn.setOnClickListener {
            clickListener?.onSignInClicked()
        }
        binding.buttonRestoreBackup.setOnClickListener {
            clickListener?.onRestoreBackupClicked()
        }
        binding.buttonDismiss.setOnClickListener {
            setFirstRunDismissed()
            clickListener?.onDismissClicked()
        }

        binding.containerErrorReports.setOnClickListener {
            // New state is inversion of current state.
            val isSendErrorReports = !binding.checkboxErrorReports.isChecked
            AppSettings.setSendErrorReports(context, isSendErrorReports, true)
            binding.checkboxErrorReports.isChecked = isSendErrorReports
        }
        binding.checkboxErrorReports.isChecked = AppSettings.isSendErrorReports(context)
        binding.checkboxErrorReports.text = TextTools.buildTextAppearanceSpan(
            context,
            R.string.pref_error_reports,
            R.style.TextAppearance_SeriesGuide_Subtitle1_Secondary
        )

        ViewTools.openUriOnClick(
            binding.textViewPolicyLink,
            context.getString(R.string.url_privacy)
        )
    }

    private fun setFirstRunDismissed() {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(PREF_KEY_FIRSTRUN, true)
        }
    }

    companion object {

        const val PREF_KEY_FIRSTRUN = "accepted_eula"

        @JvmStatic
        fun hasSeenFirstRunFragment(context: Context): Boolean {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            return sp.getBoolean(PREF_KEY_FIRSTRUN, false)
        }
    }
}
