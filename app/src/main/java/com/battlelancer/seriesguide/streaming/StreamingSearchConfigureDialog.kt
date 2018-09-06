package com.battlelancer.seriesguide.streaming

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import org.greenrobot.eventbus.EventBus

class StreamingSearchConfigureDialog : AppCompatDialogFragment() {

    data class StreamingSearchConfiguredEvent(val turnedOff: Boolean)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val services = StreamingSearch.serviceToUrl.keys.toList()

        val serviceOrEmptyOrNull = StreamingSearch.getServiceOrEmptyOrNull(requireContext())
        val currentSelection = when {
            serviceOrEmptyOrNull == null -> -1 // not configured
            serviceOrEmptyOrNull.isEmpty() -> 0 // turned off
            else -> services.indexOf(serviceOrEmptyOrNull) + 1
        }

        val items = Array<CharSequence>(1 + services.size) { i ->
            if (i == 0) {
                getString(R.string.action_turn_off)
            } else {
                StreamingSearch.getServiceDisplayName(services[i - 1])
            }
        }

        return AlertDialog.Builder(requireContext())
                .setTitle(R.string.action_stream)
                .setSingleChoiceItems(
                        items,
                        currentSelection
                ) { _, position ->
                    val countryOrEmpty = if (position == 0) {
                        ""
                    } else {
                        services[position - 1]
                    }
                    StreamingSearch.setServiceOrEmpty(requireContext(), countryOrEmpty)
                    EventBus.getDefault().post(StreamingSearchConfiguredEvent(position == 0))
                    dismiss()
                }.create()
    }

    companion object {
        @JvmStatic
        fun show(fragmentManager: FragmentManager) {
            StreamingSearchConfigureDialog().show(fragmentManager, "streamingSearchDialog");
        }
    }

}