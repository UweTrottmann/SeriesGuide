// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogYearPickerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.threeten.bp.LocalDate

/**
 * Dialog that allows to pick a year. Get notified once the dialog closes with [onPickedListener].
 */
class YearPickerDialogFragment : AppCompatDialogFragment() {

    interface OnPickedListener {
        /**
         * If no year was selected null is returned.
         */
        fun onPicked(year: Int?)
    }

    var onPickedListener: OnPickedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogYearPickerBinding.inflate(layoutInflater)

        val initialYear = requireArguments().getInt(ARG_INITIAL_YEAR)

        binding.numberPickerYear.apply {
            val currentDateTime = LocalDate.now()
            minValue = MINIMUM_YEAR
            maxValue = currentDateTime.plusYears(MAXIMUM_YEAR_OFFSET).year
            value = initialYear.let { if (it != 0) it else currentDateTime.year }
        }

        val yearPicker = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(R.string.filter_year)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onPickedListener?.onPicked(binding.numberPickerYear.value)
            }
            .setNegativeButton(R.string.action_reset) { _, _ ->
                onPickedListener?.onPicked(null)
            }
            .create()
        return yearPicker
    }

    companion object {
        private const val MINIMUM_YEAR = 1980
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