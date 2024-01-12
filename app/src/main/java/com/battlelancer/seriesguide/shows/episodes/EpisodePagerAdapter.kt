// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.episodes

import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers
import com.battlelancer.seriesguide.util.TextTools

/**
 * Maps [Episode] objects to [EpisodeDetailsFragment] pages.
 */
@Suppress("DEPRECATION") // ViewPager2 paging performance is bad, don't need its improvements
@SuppressLint("WrongConstant") // Behavior flag not recognized as valid.
internal class EpisodePagerAdapter(
    private val context: Context,
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val episodes = ArrayList<SgEpisode2Numbers>()

    override fun getCount(): Int {
        return episodes.size
    }

    override fun getItem(position: Int): Fragment {
        return EpisodeDetailsFragment.newInstance(episodes[position].id)
    }

    override fun getItemPosition(`object`: Any): Int {
        /*
         * This breaks the FragmentStatePagerAdapter
         * (see https://issuetracker.google.com/issues/36956111),
         * so just destroy everything!
         * Note: This might be fixed with ViewPager2.
         */
        // EpisodeDetailsFragment fragment = (EpisodeDetailsFragment)
        // object;
        // int episodeId = fragment.getEpisodeId();
        // for (int i = 0; i < mEpisodes.size(); i++) {
        // if (episodeId == mEpisodes.get(i).episodeId) {
        // return i;
        // }
        // }
        return POSITION_NONE
    }

    fun getItemEpisodeId(position: Int): Long? {
        return if (position < episodes.size) {
            episodes[position].id
        } else {
            null
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val episode = episodes[position]
        return TextTools.getEpisodeNumber(context, episode.season, episode.episodenumber)
    }

    fun updateEpisodeList(list: List<SgEpisode2Numbers>) {
        episodes.clear()
        episodes.addAll(list)
        notifyDataSetChanged()
    }
}
