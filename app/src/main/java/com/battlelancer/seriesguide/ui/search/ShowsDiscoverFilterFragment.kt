package com.battlelancer.seriesguide.ui.search

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentShowsDiscoverFilterBinding
import com.battlelancer.seriesguide.model.SgWatchProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.streaming.StreamingSearchConfigureDialog
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Displays filter options for the new episodes or popular shows list,
 * currently watch provider in a specific region only.
 */
class ShowsDiscoverFilterFragment : AppCompatDialogFragment() {

    private val model: ShowsDiscoverFilterViewModel by viewModels()
    private var binding: FragmentShowsDiscoverFilterBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentShowsDiscoverFilterBinding.inflate(layoutInflater)
            .also { binding = it }

        // watch region button
        binding.buttonWatchRegion.apply {
            text = StreamingSearch.getCurrentRegionOrSelectString(requireContext())
            setOnClickListener { StreamingSearchConfigureDialog.show(parentFragmentManager) }
        }
        StreamingSearch.regionLiveData.observe(this, {
            this.binding?.buttonWatchRegion?.text =
                StreamingSearch.getCurrentRegionOrSelectString(requireContext())
            val language = DisplaySettings.getShowsSearchLanguage(requireContext())
            if (it != null) model.updateWatchProviders(language, it)
        })

        // disable all button
        binding.buttonDisableAllProviders.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                SgRoomDatabase.getInstance(requireContext()).sgWatchProviderHelper()
                    .setAllDisabled(SgWatchProvider.Type.SHOWS.id)
            }
        }

        // watch provider list
        val adapter = ShowsDiscoverFilterAdapter(watchProviderClickListener)
        binding.recyclerViewWatchProviders.also {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.adapter = adapter
        }

        // Note: DialogFragment does not have viewLifecycleOwner.
        lifecycleScope.launch {
            model.allWatchProvidersFlow.collectLatest {
                adapter.submitData(it)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_stream)
            .setView(binding.root)
            .setPositiveButton(R.string.dismiss, null)
            .create()
    }

    private val watchProviderClickListener = object : ShowsDiscoverFilterAdapter.ClickListener {
        override fun onClick(watchProvider: SgWatchProvider) {
            // Note: DialogFragment does not have viewLifecycleOwner.
            lifecycleScope.launch(Dispatchers.IO) {
                SgRoomDatabase.getInstance(requireContext()).sgWatchProviderHelper()
                    .setEnabled(watchProvider._id, !watchProvider.enabled)
            }
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager) =
            ShowsDiscoverFilterFragment().safeShow(fragmentManager, "shows-discover-filter")
    }

}