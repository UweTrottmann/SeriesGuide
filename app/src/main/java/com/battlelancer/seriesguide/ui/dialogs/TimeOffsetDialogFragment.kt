package com.battlelancer.seriesguide.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogTimeOffsetBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.getShowsTimeOffset
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import timber.log.Timber

/**
 * Dialog which allows to select the number of hours (between +/-24)
 * to offset show release times by.
 */
class TimeOffsetDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogTimeOffsetBinding? = null

    private var hours = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTimeOffsetBinding.inflate(layoutInflater)
        this.binding = binding

        binding.textViewOffsetRange.text = getString(R.string.format_time_offset_range, -24, 24)
        binding.editTextOffsetValue.hint = getString(R.string.format_time_offset_range, -24, 24)
        binding.editTextOffsetValue.addTextChangedListener(textWatcher)

        val hours = getShowsTimeOffset(requireContext())
        binding.editTextOffsetValue.setText(hours.toString())
        // text views are updated by text watcher

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_offset)
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

        // do only allow values between +/-24
        var resetValue = false
        if (value < -24) {
            resetValue = true
            value = -24
        } else if (value > 24) {
            resetValue = true
            value = 24
        }

        if (resetValue) {
            s.replace(0, s.length, value.toString())
        }

        hours = value
        updateSummaryAndExample(value)
    }

    @SuppressLint("SetTextI18n")
    private fun updateSummaryAndExample(value: Int) {
        val binding = binding ?: return

        binding.textViewOffsetSummary.text = getString(R.string.pref_offsetsummary, value)
        val original = LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 0))
        val offset = original.plusHours(value.toLong())
        binding.textViewOffsetSummary.text =
            formatToTimeString(original).toString() + " -> " + formatToTimeString(offset)
    }

    private fun formatToTimeString(localDateTime: LocalDateTime): CharSequence {
        return DateUtils.getRelativeDateTimeString(
            requireContext(),
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0
        )
    }

    private fun saveAndDismiss() {
        val hours = hours
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
            .putString(DisplaySettings.KEY_SHOWS_TIME_OFFSET, hours.toString())
            .apply()
        Timber.i("Time offset set to %d hours", hours)
        dismiss()
    }
}