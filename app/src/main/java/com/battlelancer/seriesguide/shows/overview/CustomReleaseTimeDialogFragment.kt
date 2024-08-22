// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.overview

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogCustomReleaseTimeBinding
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Displays original release time and day and allows to change the release time and specify a day
 * offset.
 */
class CustomReleaseTimeDialogFragment() : AppCompatDialogFragment() {

    constructor(showId: Long) : this() {
        arguments = bundleOf(
            ARG_SHOW_ID to showId
        )
    }

    private val model: CustomReleaseTimeDialogModel by viewModels(
        extrasProducer = {
            CustomReleaseTimeDialogModel.creationExtras(
                defaultViewModelCreationExtras,
                requireArguments().getLong(ARG_SHOW_ID)
            )
        },
        factoryProducer = { CustomReleaseTimeDialogModel.Factory }
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCustomReleaseTimeBinding.inflate(layoutInflater)
        binding.setViewsEnabled(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.customTimeDataWithStrings.collectLatest {
                    updatePreview(binding, it)
                }
            }
        }

        binding.buttonCustomReleaseTimePick.setOnClickListener {
            val customTimeInfo = model.customTimeDataWithStrings.value?.customTimeData
                ?: return@setOnClickListener

            val isSystem24Hour = is24HourFormat(context)
            val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

            val picker =
                MaterialTimePicker.Builder()
                    .setTimeFormat(clockFormat)
                    .setHour(customTimeInfo.customTime.hour)
                    .setMinute(customTimeInfo.customTime.minute)
                    .setTitleText(R.string.custom_release_time_edit)
                    .build()
            picker.addOnPositiveButtonClickListener {
                model.updateTime(picker.hour, picker.minute)
            }
            picker.safeShow(parentFragmentManager, "custom-time-picker")
        }
        binding.buttonCustomReleaseTimeOffsetDecrease.setOnClickListener {
            model.decreaseDayOffset()
        }
        binding.buttonCustomReleaseTimeOffsetIncrease.setOnClickListener {
            model.increaseDayOffset()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.custom_release_time_edit)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                model.saveToDatabase()
            }
            .setNegativeButton(android.R.string.cancel, null /* just dismiss */)
            .setNeutralButton(R.string.action_reset) { _, _ ->
                model.resetToOfficialAndSave()
                dismiss()
            }
            .create()
    }

    private fun DialogCustomReleaseTimeBinding.setViewsEnabled(isEnabled: Boolean) {
        buttonCustomReleaseTimePick.isEnabled = isEnabled
        buttonCustomReleaseTimeOffsetIncrease.isEnabled = isEnabled
        buttonCustomReleaseTimeOffsetDecrease.isEnabled = isEnabled
    }


    private fun updatePreview(
        binding: DialogCustomReleaseTimeBinding,
        dataWithStrings: CustomTimeDataWithStrings?
    ) {
        if (dataWithStrings == null) {
            binding.setViewsEnabled(false)
        } else {
            binding.textViewCustomReleaseTimeDay.text = dataWithStrings.customDayString
            binding.buttonCustomReleaseTimePick.text = dataWithStrings.customTimeString
            binding.textViewCustomReleaseTimeOffset.text = dataWithStrings.customDayOffsetString
            binding.textViewCustomReleaseTimeOffsetDirection.text =
                dataWithStrings.customDayOffsetDirectionString
            binding.setViewsEnabled(true)
        }
    }

    companion object {
        private const val ARG_SHOW_ID = "showid"
    }

}