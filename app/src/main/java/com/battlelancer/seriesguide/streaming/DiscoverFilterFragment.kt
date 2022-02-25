package com.battlelancer.seriesguide.streaming

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentShowsDiscoverFilterBinding
import com.battlelancer.seriesguide.model.SgWatchProvider
import com.battlelancer.seriesguide.model.SgWatchProvider.Type
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.search.ShowsDiscoverFilterAdapter
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dialog to configure watch provider list in a specific region only to filter by,
 * stores different list for shows or movies.
 */
class DiscoverFilterFragment : AppCompatDialogFragment() {

    private lateinit var type: Type
    private val model: DiscoverFilterViewModel by viewModels {
        DiscoverFilterViewModelFactory(requireActivity().application, type)
    }
    private var binding: FragmentShowsDiscoverFilterBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val typeId = requireArguments().getInt(ARG_TYPE)
        type = when (typeId) {
            Type.SHOWS.id -> Type.SHOWS
            Type.MOVIES.id -> Type.MOVIES
            else -> throw IllegalArgumentException("unknown type id $typeId")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentShowsDiscoverFilterBinding.inflate(layoutInflater)
            .also { binding = it }

        // watch region button
        binding.buttonWatchRegion.apply {
            text = StreamingSearch.getCurrentRegionOrSelectString(requireContext())
            setOnClickListener { StreamingSearchConfigureDialog.show(parentFragmentManager) }
        }
        StreamingSearch.regionLiveData.observe(this) {
            this.binding?.buttonWatchRegion?.text =
                StreamingSearch.getCurrentRegionOrSelectString(requireContext())
            if (it != null) model.updateWatchProviders(it)
        }

        // disable all button
        binding.buttonDisableAllProviders.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                SgRoomDatabase.getInstance(requireContext()).sgWatchProviderHelper()
                    .setAllDisabled(type.id)
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

        val titleRes = when (type) {
            Type.SHOWS -> R.string.action_shows_filter
            Type.MOVIES -> R.string.action_movies_filter
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
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
        private const val ARG_TYPE = "type"

        fun showForShows(fragmentManager: FragmentManager) =
            DiscoverFilterFragment()
                .apply { arguments = bundleOf(ARG_TYPE to Type.SHOWS.id) }
                .safeShow(fragmentManager, "shows-discover-filter")

        fun showForMovies(fragmentManager: FragmentManager) =
            DiscoverFilterFragment()
                .apply { arguments = bundleOf(ARG_TYPE to Type.MOVIES.id) }
                .safeShow(fragmentManager, "movies-discover-filter")
    }

}