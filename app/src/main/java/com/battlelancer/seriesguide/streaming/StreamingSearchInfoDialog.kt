package com.battlelancer.seriesguide.streaming

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogStreamingInfoBinding
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Displays current region for streaming search info, attributes streaming search info.
 */
class StreamingSearchInfoDialog : AppCompatDialogFragment() {

    private var binding: DialogStreamingInfoBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogStreamingInfoBinding.inflate(layoutInflater)
        this.binding = binding

        binding.also {
            it.buttonStreamingInfoRegion.text =
                StreamingSearch.getCurrentRegionOrSelectString(requireContext())
            it.buttonStreamingInfoRegion.setOnClickListener {
                StreamingSearchConfigureDialog.show(parentFragmentManager)
            }
        }

        StreamingSearch.regionLiveData.observe(this, {
            this.binding?.buttonStreamingInfoRegion?.text =
                StreamingSearch.getCurrentRegionOrSelectString(requireContext())
        })

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_stream)
            .setView(binding.root)
            .setPositiveButton(R.string.dismiss, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        @JvmStatic
        fun show(fragmentManager: FragmentManager) {
            StreamingSearchInfoDialog().safeShow(fragmentManager, "streamingSearchInfoDialog")
        }
    }

}