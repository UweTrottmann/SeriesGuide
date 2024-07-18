// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogYearPickerBinding
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment.Companion.MINIMUM_YEAR
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment.Companion.YEAR_CURRENT
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.threeten.bp.LocalDate

/**
 * Dialog that allows to pick a year (at least [MINIMUM_YEAR]), no year or [YEAR_CURRENT].
 *
 * Get notified once the dialog closes with [onPickedListener].
 */
class YearPickerDialogFragment : AppCompatDialogFragment() {

    interface OnPickedListener {
        /**
         * If no year was selected returns `null`.
         *
         * If the current year should be computed returns [YEAR_CURRENT].
         */
        fun onPicked(year: Int?)
    }

    var onPickedListener: OnPickedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogYearPickerBinding.inflate(layoutInflater)

        val initialYear = requireArguments().getInt(ARG_INITIAL_YEAR)

        val currentDateTime = LocalDate.now()
        binding.numberPickerYear.apply {
            minValue = MINIMUM_YEAR
            maxValue = currentDateTime.plusYears(MAXIMUM_YEAR_OFFSET).year
            value =
                initialYear.let { if (it != 0 && it != YEAR_CURRENT) it else currentDateTime.year }
        }
        binding.switchYearCurrent.apply {
            setOnCheckedChangeListener { _, isChecked ->
                binding.numberPickerYear.isEnabled = !isChecked
                if (isChecked) binding.numberPickerYear.value = currentDateTime.year
            }
            isChecked = initialYear == YEAR_CURRENT
        }

        val yearPicker = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(R.string.filter_year)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val pickedYear =
                    if (binding.switchYearCurrent.isChecked) {
                        YEAR_CURRENT
                    } else {
                        binding.numberPickerYear.value
                    }
                onPickedListener?.onPicked(pickedYear)
            }
            .setNegativeButton(R.string.action_reset) { _, _ ->
                onPickedListener?.onPicked(null)
            }
            .create()
        return yearPicker
    }

    companion object {
        /**
         * Special return value to indicate the current year should be computed.
         */
        const val YEAR_CURRENT = 1
        const val MINIMUM_YEAR = 1980
        private const val MAXIMUM_YEAR_OFFSET = 1L

        private const val ARG_INITIAL_YEAR = "initialYear"

        fun create(initialYear: Int?) = YearPickerDialogFragment().apply {
            arguments = Bundle().apply {
                if (initialYear != null) {
                    putInt(ARG_INITIAL_YEAR, initialYear)
                }
            }
        }
    }

}

/**
 * If this is [YEAR_CURRENT], converts to current year.
 */
fun Int?.toActualYear(): Int? =
    if (this == YEAR_CURRENT) {
        LocalDate.now().year
    } else this
