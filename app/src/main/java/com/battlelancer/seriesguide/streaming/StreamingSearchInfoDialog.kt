// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.streaming

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogStreamingInfoBinding
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Displays current region for streaming search info, attributes streaming search info.
 */
class StreamingSearchInfoDialog : AppCompatDialogFragment() {

    private var binding: DialogStreamingInfoBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogStreamingInfoBinding.inflate(layoutInflater)
        this.binding = binding

        binding.also {
            it.buttonStreamingInfoRegion.setOnClickListener {
                StreamingSearchConfigureDialog.show(parentFragmentManager)
            }
        }

        // Set and update the region selection button text
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.i("Updating region button text")
                StreamingSearch.regionCode.collect {
                    binding.buttonStreamingInfoRegion.text =
                        StreamingSearch.getCurrentRegionOrSelectString(requireContext())
                }
            }
        }

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