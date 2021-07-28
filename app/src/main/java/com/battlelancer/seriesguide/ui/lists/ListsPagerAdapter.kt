package com.battlelancer.seriesguide.ui.lists

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.battlelancer.seriesguide.model.SgList

/**
 * Returns [SgListFragment] for every list.
 */
class ListsPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    private val items = mutableListOf<SgList>()

    @SuppressLint("NotifyDataSetChanged") // No need for incremental updates/animations.
    fun updateItems(newItems: List<SgList>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItemListId(position: Int): String? {
        return if (position < items.size) {
            items[position].listId
        } else {
            null
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < items.size) {
            items[position].id.toLong()
        } else {
            RecyclerView.NO_ID
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return items.find { it.id.toLong() == itemId } != null
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun createFragment(position: Int): Fragment {
        return SgListFragment.newInstance(items[position].listId, position)
    }

}