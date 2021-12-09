package com.battlelancer.seriesguide.ui.episodes

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers

/**
 * Maps [Episode] objects to [EpisodeDetailsFragment] pages.
 */
class EpisodePagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val items = ArrayList<SgEpisode2Numbers>()

    @SuppressLint("NotifyDataSetChanged") // No need for incremental updates/animations.
    fun updateItems(list: List<SgEpisode2Numbers>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getItemEpisodeId(position: Int): Long? {
        return if (position < items.size) {
            items[position].id
        } else {
            null
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < items.size) {
            items[position].id
        } else {
            RecyclerView.NO_ID
        }
    }

    override fun containsItem(itemId: Long): Boolean = items.find { it.id == itemId } != null

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment =
        EpisodeDetailsFragment.newInstance(items[position].id)

}
