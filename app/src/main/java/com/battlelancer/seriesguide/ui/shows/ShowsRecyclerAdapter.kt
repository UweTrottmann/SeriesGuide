package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.content.res.Resources
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.util.ViewTools

class ShowsRecyclerAdapter(
    private val context: Context,
    drawableTheme: Resources.Theme,
    private val onItemClickListener: OnItemClickListener
) :
    ListAdapter<SgShow, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    private val drawableStar: VectorDrawableCompat = ViewTools.vectorIconActive(
        context, drawableTheme,
        R.drawable.ic_star_black_24dp
    )
    private val drawableStarZero: VectorDrawableCompat = ViewTools.vectorIconActive(
        context, drawableTheme,
        R.drawable.ic_star_border_black_24dp
    )

    interface OnItemClickListener {
        fun onItemClick(anchor: View, showTvdbId: Int)

        fun onItemMenuClick(anchor: View, viewHolder: ShowsViewHolder)

        fun onItemFavoriteClick(showTvdbId: Int, isFavorite: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ShowsViewHolder.create(parent, onItemClickListener, drawableStar, drawableStarZero)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ShowsViewHolder) {
            holder.bind(context, getItem(position))
        } else {
            throw IllegalArgumentException("Unknown view holder type")
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SgShow>() {
            override fun areItemsTheSame(old: SgShow, new: SgShow): Boolean =
                old.tvdbId == new.tvdbId

            override fun areContentsTheSame(old: SgShow, new: SgShow): Boolean {
                return old.title == new.title && old.favorite == new.favorite
            }

        }
    }
}