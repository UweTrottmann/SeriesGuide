package com.battlelancer.seriesguide.streaming

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Single choice of regions, selected region set to [StreamingSearch.regionLiveData].
 */
class StreamingSearchConfigureDialog : AppCompatDialogFragment() {

    data class RegionItem(
        val code: String,
        val displayText: String
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val regions = StreamingSearch.supportedRegions

        val regionItems = List(regions.size) { i ->
            RegionItem(regions[i], StreamingSearch.getServiceDisplayName(regions[i]))
        }.sortedBy { it.displayText }

        val currentSelection =
            when (val regionOrNull = StreamingSearch.getCurrentRegionOrNull(requireContext())) {
                null -> -1 // not configured
                else -> regionItems.indexOfFirst { it.code == regionOrNull }
            }

        val items = Array<CharSequence>(regionItems.size) { i ->
            regionItems[i].displayText
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_select_region)
            .setSingleChoiceItems(
                items,
                currentSelection
            ) { _, position ->
                val region = regionItems[position].code
                StreamingSearch.setRegion(requireContext(), region)
                dismiss()
            }.create()
    }

    companion object {
        @JvmStatic
        fun show(fragmentManager: FragmentManager) {
            StreamingSearchConfigureDialog().safeShow(fragmentManager, "streamingSearchDialog")
        }
    }

}