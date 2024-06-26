// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2020, 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesCollectionBinding

/**
 * Displays the users collection of movies.
 */
class MoviesCollectionFragment : MoviesBaseFragment() {

    override val emptyViewTextResId = R.string.movies_collection_empty

    private val _model by viewModels<MoviesCollectionViewModel>()

    override val model: MoviesWatchedViewModel
        get() = _model

    override val recyclerView: RecyclerView
        get() = binding!!.recyclerViewMoviesCollection

    override val emptyView: TextView
        get() = binding!!.textViewMoviesCollectionEmpty

    private var binding: FragmentMoviesCollectionBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentMoviesCollectionBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun getTabPosition(showingNowTab: Boolean): Int {
        return if (showingNowTab) {
            MoviesActivityImpl.TAB_POSITION_COLLECTION_WITH_HISTORY
        } else {
            MoviesActivityImpl.TAB_POSITION_COLLECTION_DEFAULT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewMoviesCollection
    }
}
