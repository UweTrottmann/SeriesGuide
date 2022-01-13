package com.battlelancer.seriesguide.ui.dialogs

import android.content.res.Resources
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.PluralsRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.NotificationSettings
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Dialog which allows to select the number of minutes, hours or days when a notification should
 * appear before an episode is released.
 */
class NotificationThresholdDialogFragment : AppCompatDialogFragment() {

    @BindView(R.id.buttonNegative)
    var buttonNegative: View? = null

    @BindView(R.id.buttonPositive)
    var buttonPositive: Button? = null

    @BindView(R.id.editTextThresholdValue)
    var editTextValue: EditText? = null

    @BindView(R.id.radioGroupThreshold)
    var radioGroup: RadioGroup? = null

    @BindView(R.id.radioButtonThresholdMinutes)
    var radioButtonMinutes: RadioButton? = null

    @BindView(R.id.radioButtonThresholdHours)
    var radioButtonHours: RadioButton? = null

    @BindView(R.id.radioButtonThresholdDays)
    var radioButtonDays: RadioButton? = null
    private var unbinder: Unbinder? = null
    private var value = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_notification_threshold, container, false)
        unbinder = ButterKnife.bind(this, view)

        buttonNegative!!.visibility = View.GONE
        buttonPositive!!.setText(android.R.string.ok)
        buttonPositive!!.setOnClickListener { saveAndDismiss() }

        editTextValue!!.addTextChangedListener(textWatcher)

        radioGroup!!.setOnCheckedChangeListener { _: RadioGroup?, _: Int ->
            // trigger text watcher, takes care of validating the value based on the new unit
            editTextValue!!.text = editTextValue!!.text
        }

        bindViews()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    private fun bindViews() {
        val minutes = NotificationSettings.getLatestToIncludeTreshold(requireContext())
        val value: Int
        if (minutes != 0 && minutes % (24 * 60) == 0) {
            value = minutes / (24 * 60)
            radioGroup!!.check(R.id.radioButtonThresholdDays)
        } else if (minutes != 0 && minutes % 60 == 0) {
            value = minutes / 60
            radioGroup!!.check(R.id.radioButtonThresholdHours)
        } else {
            value = minutes
            radioGroup!!.check(R.id.radioButtonThresholdMinutes)
        }
        editTextValue!!.setText(value.toString())
        // radio buttons are updated by text watcher
    }

    private fun parseAndUpdateValue(s: Editable) {
        var value = 0
        try {
            value = s.toString().toInt()
        } catch (ignored: NumberFormatException) {
        }

        // do not allow values bigger than a threshold, based on the selected unit
        var resetValue = false
        if (radioGroup!!.checkedRadioButtonId == radioButtonMinutes!!.id
            && value > 600) {
            resetValue = true
            value = 600
        } else if (radioGroup!!.checkedRadioButtonId == radioButtonHours!!.id
            && value > 120) {
            resetValue = true
            value = 120
        } else if (radioGroup!!.checkedRadioButtonId == radioButtonDays!!.id
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
        val placeholderPattern = Pattern.compile("%d\\s*")
        val res = resources
        radioButtonMinutes!!.text = getQuantityStringWithoutPlaceholder(
            placeholderPattern, res, R.plurals.minutes_before_plural, value
        )
        radioButtonHours!!.text = getQuantityStringWithoutPlaceholder(
            placeholderPattern, res, R.plurals.hours_before_plural, value
        )
        radioButtonDays!!.text = getQuantityStringWithoutPlaceholder(
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
        var minutes = value

        // if not already, convert to minutes
        if (radioGroup!!.checkedRadioButtonId == radioButtonHours!!.id) {
            minutes *= 60
        } else if (radioGroup!!.checkedRadioButtonId == radioButtonDays!!.id) {
            minutes *= 60 * 24
        }

        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
            .putString(NotificationSettings.KEY_THRESHOLD, minutes.toString())
            .apply()
        Timber.i("Notification threshold set to %d minutes", minutes)

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