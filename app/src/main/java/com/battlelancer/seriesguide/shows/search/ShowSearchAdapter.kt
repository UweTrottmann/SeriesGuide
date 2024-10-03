// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemShowBinding
import com.battlelancer.seriesguide.shows.database.SgShow2ForLists
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TimeTools.formatWithDeviceZoneToDayAndTime

/**
 * Display show search results.
 */
class ShowSearchAdapter(
    context: Context,
    private val itemClickListener: ItemClickListener
) : ArrayAdapter<SgShow2ForLists>(context, 0) {

    interface ItemClickListener {
        fun onItemClick(anchor: View, showId: Long)
        fun onMoreOptionsClick(anchor: View, show: SgShow2ForLists)
        fun onFavoriteClick(showId: Long, isFavorite: Boolean)
    }

    private val drawableStar = VectorDrawableCompat
        .create(context.resources, R.drawable.ic_star_black_24dp, context.theme)
    private val drawableStarZero = VectorDrawableCompat
        .create(context.resources, R.drawable.ic_star_border_black_24dp, context.theme)

    fun setData(data: List<SgShow2ForLists>?) {
        clear()
        data?.also {
            addAll(it)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder = if (convertView == null) {
            ShowSearchViewHolder.create(parent, itemClickListener, drawableStar, drawableStarZero)
                .also { it.binding.root.tag = it }
        } else {
            convertView.tag as ShowSearchViewHolder
        }

        val item: SgShow2ForLists? = getItem(position)
        if (item != null) {
            viewHolder.bindTo(item, context)
        }

        return viewHolder.binding.root
    }

    class ShowSearchViewHolder(
        val binding: ItemShowBinding,
        private val itemClickListener: ItemClickListener,
        private val drawableStar: Drawable?,
        private val drawableStarZero: Drawable?
    ) {

        var show: SgShow2ForLists? = null

        init {
            // item
            binding.root.setOnClickListener { view: View ->
                show?.let {
                    itemClickListener.onItemClick(view, it.id)
                }
            }
            // favorite star
            binding.imageViewItemShowFavorited.setOnClickListener {
                show?.let {
                    itemClickListener.onFavoriteClick(it.id, !it.favorite)
                }
            }
            // more options button
            binding.imageViewShowMoreOptions.setOnClickListener {
                show?.let {
                    itemClickListener.onMoreOptionsClick(binding.imageViewShowMoreOptions, it)
                }
            }
        }

        fun bindTo(show: SgShow2ForLists, context: Context) {
            this.show = show

            // show title
            binding.textViewItemShowTitle.text = show.title

            // favorited label
            setFavoriteState(binding.imageViewItemShowFavorited, show.favorite)

            // network, day and time
            binding.textViewItemShowTimeAndNetwork.text = TextTools.dotSeparate(
                show.network,
                TimeTools.getReleaseDateTime(context, show)
                    ?.formatWithDeviceZoneToDayAndTime()
            )
            binding.textViewItemShowRemaining.visibility = View.GONE // unused

            // poster
            ImageTools.loadShowPosterResizeCrop(
                context,
                binding.imageViewItemShowPoster,
                show.posterSmall
            )
        }

        private fun setFavoriteState(view: ImageView, isFavorite: Boolean) {
            view.setImageDrawable(if (isFavorite) drawableStar else drawableStarZero)
            view.contentDescription =
                view.context.getString(if (isFavorite) R.string.context_unfavorite else R.string.context_favorite)
        }

        companion object {
            fun create(
                parent: ViewGroup,
                itemClickListener: ItemClickListener,
                drawableStar: Drawable?,
                drawableStarZero: Drawable?
            ): ShowSearchViewHolder {
                return ShowSearchViewHolder(
                    ItemShowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    itemClickListener, drawableStar, drawableStarZero
                )
            }
        }
    }

}