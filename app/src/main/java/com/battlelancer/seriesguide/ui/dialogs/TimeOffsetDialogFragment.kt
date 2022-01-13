package com.battlelancer.seriesguide.ui.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.getShowsTimeOffset
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

    @BindView(R.id.buttonNegative)
    var buttonNegative: View? = null

    @BindView(R.id.buttonPositive)
    var buttonPositive: Button? = null

    @BindView(R.id.editTextOffsetValue)
    var editTextValue: EditText? = null

    @BindView(R.id.textViewOffsetRange)
    var textViewRange: TextView? = null

    @BindView(R.id.textViewOffsetSummary)
    var textViewSummary: TextView? = null

    @BindView(R.id.textViewOffsetExample)
    var textViewExample: TextView? = null
    private var unbinder: Unbinder? = null

    private var hours = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_time_offset, container, false)
        unbinder = ButterKnife.bind(this, view)

        buttonNegative!!.visibility = View.GONE
        buttonPositive!!.setText(android.R.string.ok)
        buttonPositive!!.setOnClickListener { v: View? -> saveAndDismiss() }

        textViewRange!!.text = getString(R.string.format_time_offset_range, -24, 24)
        editTextValue!!.hint = getString(R.string.format_time_offset_range, -24, 24)

        editTextValue!!.addTextChangedListener(textWatcher)

        bindViews()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    private fun bindViews() {
        val hours = getShowsTimeOffset(requireContext())
        editTextValue!!.setText(hours.toString())
        // text views are updated by text watcher
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
        textViewSummary!!.text = getString(R.string.pref_offsetsummary, value)
        val original = LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 0))
        val offset = original.plusHours(value.toLong())
        textViewExample!!.text =
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

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            parseAndUpdateValue(s)
        }
    }
}