package com.battlelancer.seriesguide.ui.shows

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
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.UpdateSettings
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.Utils
import org.greenrobot.eventbus.EventBus

class FirstRunView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    enum class ButtonType(val id: Int) {
        DISMISS(0),
        ADD_SHOW(1),
        SIGN_IN(2),
        RESTORE_BACKUP(3)
    }

    class ButtonEvent(val type: ButtonType)

    private val binding: ViewFirstRunBinding =
        ViewFirstRunBinding.inflate(LayoutInflater.from(context), this, true)

    override fun onFinishInflate() {
        super.onFinishInflate()

        binding.groupAutoBackupDetected.isGone =
            !AutoBackupTools.isAutoBackupMaybeAvailable(context)
        binding.buttonRestoreAutoBackup.setOnClickListener {
            context.startActivity(DataLiberationActivity.intentToShowAutoBackup(context))
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
            EventBus.getDefault().post(ButtonEvent(ButtonType.ADD_SHOW))
        }
        binding.buttonSignIn.setOnClickListener {
            EventBus.getDefault().post(ButtonEvent(ButtonType.SIGN_IN))
        }
        binding.buttonRestoreBackup.setOnClickListener {
            EventBus.getDefault().post(ButtonEvent(ButtonType.RESTORE_BACKUP))
        }
        binding.buttonDismiss.setOnClickListener {
            setFirstRunDismissed()
            EventBus.getDefault().post(ButtonEvent(ButtonType.DISMISS))
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

        binding.textViewPolicyLink.setOnClickListener { v ->
            val context = v.context
            Utils.launchWebsite(context, context.getString(R.string.url_privacy))
        }
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
