// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesWatchedBinding

/**
 * Displays watched movies.
 */
class MoviesWatchedFragment : MoviesBaseFragment() {

    override val emptyViewTextResId = R.string.now_movies_empty

    private val _model by viewModels<MoviesWatchedViewModel>()

    override val model: MoviesWatchedViewModel
        get() = _model

    override val recyclerView: RecyclerView
        get() = binding!!.recyclerViewMoviesWatched

    override val emptyView: TextView
        get() = binding!!.textViewEmptyMoviesWatched

    private var binding: FragmentMoviesWatchedBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMoviesWatchedBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun getTabPosition(showingNowTab: Boolean): Int {
        return if (showingNowTab) {
            MoviesActivityImpl.TAB_POSITION_WATCHED_WITH_HISTORY
        } else {
            MoviesActivityImpl.TAB_POSITION_WATCHED_DEFAULT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewMoviesWatched

        fun newInstance() = MoviesWatchedFragment()
    }

}
