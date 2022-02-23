package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.RadioGroup
import androidx.annotation.PluralsRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogNotificationThresholdBinding
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Dialog which allows to select the number of minutes, hours or days when a notification should
 * appear before an episode is released.
 */
class NotificationThresholdDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogNotificationThresholdBinding? = null
    private var value = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNotificationThresholdBinding.inflate(layoutInflater)
        this.binding = binding

        binding.editTextThresholdValue.addTextChangedListener(textWatcher)

        binding.radioGroupThreshold.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->
            // trigger text watcher, takes care of validating the value based on the new unit
            this.binding?.editTextThresholdValue?.let { it.text = it.text }
        }

        val minutes = NotificationSettings.getLatestToIncludeTreshold(requireContext())
        val value: Int
        if (minutes != 0 && minutes % (24 * 60) == 0) {
            value = minutes / (24 * 60)
            binding.radioGroupThreshold.check(R.id.radioButtonThresholdDays)
        } else if (minutes != 0 && minutes % 60 == 0) {
            value = minutes / 60
            binding.radioGroupThreshold.check(R.id.radioButtonThresholdHours)
        } else {
            value = minutes
            binding.radioGroupThreshold.check(R.id.radioButtonThresholdMinutes)
        }
        binding.editTextThresholdValue.setText(value.toString())
        // radio buttons are updated by text watcher

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_notifications_treshold)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                saveAndDismiss()
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            parseAndUpdateValue(s)
        }
    }

    private fun parseAndUpdateValue(s: Editable) {
        var value = 0
        try {
            value = s.toString().toInt()
        } catch (ignored: NumberFormatException) {
        }

        val binding = binding ?: return

        // do not allow values bigger than a threshold, based on the selected unit
        var resetValue = false
        if (binding.radioGroupThreshold.checkedRadioButtonId == binding.radioButtonThresholdMinutes.id
            && value > 600) {
            resetValue = true
            value = 600
        } else if (binding.radioGroupThreshold.checkedRadioButtonId == binding.radioButtonThresholdHours.id
            && value > 120) {
            resetValue = true
            value = 120
        } else if (binding.radioGroupThreshold.checkedRadioButtonId == binding.radioButtonThresholdDays.id
            && value > 7) {
            resetValue = true
            value = 7
        } else if (value < 0) {
            // should never happen due to text filter, but better safe than sorry
            resetValue = true
            value = 0
        }

        if (resetValue) {
            s.replace(0, s.length, value.toString())
        }

        this.value = value
        updateRadioButtons(value)
    }

    private fun updateRadioButtons(value: Int) {
        val binding = binding ?: return
        val placeholderPattern = Pattern.compile("%d\\s*")
        val res = resources
        binding.radioButtonThresholdMinutes.text = getQuantityStringWithoutPlaceholder(
            placeholderPattern, res, R.plurals.minutes_before_plural, value
        )
        binding.radioButtonThresholdHours.text = getQuantityStringWithoutPlaceholder(
            placeholderPattern, res, R.plurals.hours_before_plural, value
        )
        binding.radioButtonThresholdDays.text = getQuantityStringWithoutPlaceholder(
            placeholderPattern, res, R.plurals.days_before_plural, value
        )
    }

    private fun getQuantityStringWithoutPlaceholder(
        pattern: Pattern,
        res: Resources,
        @PluralsRes pluralsRes: Int,
        value: Int
    ): String {
        return pattern.matcher(res.getQuantityString(pluralsRes, value)).replaceAll("")
    }

    private fun saveAndDismiss() {
        val binding = binding ?: return

        var minutes = value

        // if not already, convert to minutes
        if (binding.radioGroupThreshold.checkedRadioButtonId == binding.radioButtonThresholdHours.id) {
            minutes *= 60
        } else if (binding.radioGroupThreshold.checkedRadioButtonId == binding.radioButtonThresholdDays.id) {
            minutes *= 60 * 24
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
            .putString(NotificationSettings.KEY_THRESHOLD, minutes.toString())
            .apply()
        Timber.i("Notification threshold set to %d minutes", minutes)

        dismiss()
    }
}