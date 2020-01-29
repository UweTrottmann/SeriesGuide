package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.edit
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.UpdateSettings
import com.battlelancer.seriesguide.util.TaskManager
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

    init {
        LayoutInflater.from(context).inflate(R.layout.view_first_run, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val noSpoilerView = findViewById<RelativeLayout>(R.id.containerFirstRunNoSpoilers)
        val noSpoilerCheckBox = noSpoilerView.findViewById<CheckBox>(
            R.id.checkboxFirstRunNoSpoilers
        )
        val dataSaverContainer = findViewById<RelativeLayout>(R.id.containerFirstRunDataSaver)
        val dataSaverCheckBox = findViewById<CheckBox>(R.id.checkboxFirstRunDataSaver)
        val buttonAddShow = findViewById<Button>(R.id.buttonFirstRunAddShow)
        val buttonSignIn = findViewById<Button>(R.id.buttonFirstRunSignIn)
        val buttonRestoreBackup = findViewById<Button>(R.id.buttonFirstRunRestore)
        val textViewPrivacyPolicy = findViewById<TextView>(R.id.textViewFirstRunPrivacyLink)
        val buttonDismiss = findViewById<ImageButton>(R.id.buttonFirstRunDismiss)

        noSpoilerView.setOnClickListener { v ->
            // new state is inversion of current state
            val noSpoilers = !noSpoilerCheckBox.isChecked
            // save
            PreferenceManager.getDefaultSharedPreferences(v.context).edit {
                putBoolean(DisplaySettings.KEY_PREVENT_SPOILERS, noSpoilers)
            }
            // update next episode strings right away
            TaskManager.getInstance().tryNextEpisodeUpdateTask(v.context)
            // show
            noSpoilerCheckBox.isChecked = noSpoilers
        }
        noSpoilerCheckBox.isChecked = DisplaySettings.preventSpoilers(context)
        dataSaverContainer.setOnClickListener {
            val isSaveData = !dataSaverCheckBox.isChecked
            PreferenceManager.getDefaultSharedPreferences(it.context).edit {
                putBoolean(UpdateSettings.KEY_ONLYWIFI, isSaveData)
            }
            dataSaverCheckBox.isChecked = isSaveData
        }
        dataSaverCheckBox.isChecked = UpdateSettings.isLargeDataOverWifiOnly(context)
        buttonAddShow.setOnClickListener {
            EventBus.getDefault()
                .post(ButtonEvent(ButtonType.ADD_SHOW))
        }
        buttonSignIn.setOnClickListener {
            EventBus.getDefault()
                .post(ButtonEvent(ButtonType.SIGN_IN))
        }
        buttonRestoreBackup.setOnClickListener {
            EventBus.getDefault()
                .post(ButtonEvent(ButtonType.RESTORE_BACKUP))
        }
        buttonDismiss.setOnClickListener {
            setFirstRunDismissed()
            EventBus.getDefault().post(ButtonEvent(ButtonType.DISMISS))
        }
        textViewPrivacyPolicy.setOnClickListener { v ->
            val context = v.context
            Utils.launchWebsite(context, context.getString(R.string.url_privacy))
        }
    }

    private fun setFirstRunDismissed() {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(FirstRunView.PREF_KEY_FIRSTRUN, true)
        }
    }

    companion object {

        private const val PREF_KEY_FIRSTRUN = "accepted_eula"

        @JvmStatic
        fun hasSeenFirstRunFragment(context: Context): Boolean {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            return sp.getBoolean(PREF_KEY_FIRSTRUN, false)
        }
    }
}
