package com.battlelancer.seriesguide.justwatch

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import org.greenrobot.eventbus.EventBus

class JustWatchConfigureDialog : AppCompatDialogFragment() {

    data class JustWatchConfiguredEvent(val turnedOff: Boolean)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val countries = JustWatchSearch.countryToUrl.keys.toList()

        val countryOrEmptyOrNull = JustWatchSearch.getCountryOrEmptyOrNull(requireContext())
        val currentSelection = if (countryOrEmptyOrNull == null) {
            -1 // not configured
        } else if (countryOrEmptyOrNull.isEmpty()) {
            0 // turned off
        } else {
            countries.indexOf(countryOrEmptyOrNull) + 1
        }

        val items = Array<CharSequence>(1 + countries.size, { i ->
            if (i == 0) {
                getString(R.string.action_turn_off)
            } else {
                JustWatchSearch.getCountryDisplayName(countries[i - 1])
            }
        })

        return AlertDialog.Builder(requireContext())
                .setTitle(R.string.action_just_watch)
                .setSingleChoiceItems(
                        items,
                        currentSelection,
                        { _, position ->
                            val countryOrEmpty = if (position == 0) {
                                ""
                            } else {
                                countries[position - 1]
                            }
                            JustWatchSearch.setCountryOrEmpty(requireContext(), countryOrEmpty)
                            EventBus.getDefault().post(JustWatchConfiguredEvent(position == 0))
                            dismiss()
                        }).create()
    }

}