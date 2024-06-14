// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.history

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemHistoryBinding
import com.battlelancer.seriesguide.util.ViewTools

/**
 * Sectioned adapter displaying recently watched episodes and episodes recently watched by
 * friends.
 */
open class ShowsHistoryAdapter(
    protected val context: Context,
    private val listener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    class MoreViewHolder(parent: ViewGroup, listener: ItemClickListener) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_now_more, parent, false)
    ) {
        val title: TextView = itemView.findViewById(R.id.textViewNowMoreText)

        init {
            itemView.setOnClickListener { v: View ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(v, position)
                }
            }
        }
    }

    class HeaderViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grid_header, parent, false)
    ) {
        private val textViewGridHeader: TextView = itemView.findViewById(R.id.textViewGridHeader)

        fun bind(title: String?, showTraktLogo: Boolean) {
            textViewGridHeader.text = title
            if (showTraktLogo) {
                ViewTools.setVectorDrawableLeft(
                    textViewGridHeader,
                    R.drawable.ic_trakt_icon_primary_20dp
                )
            } else {
                textViewGridHeader.setCompoundDrawables(null, null, null, null)
            }
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ItemType.HISTORY, ItemType.FRIEND, ItemType.MORE_LINK, ItemType.HEADER)
    annotation class ItemType {
        companion object {
            const val HISTORY = 1
            const val FRIEND = 2
            const val MORE_LINK = 3
            const val HEADER = 4
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ViewType.HISTORY, ViewType.MORE_LINK, ViewType.HEADER)
    annotation class ViewType {
        companion object {
            const val HISTORY = 1
            const val MORE_LINK = 2
            const val HEADER = 3
        }
    }

    class Item {
        var episodeRowId: Long? = null
        var showTmdbId: Int? = null
        var movieTmdbId: Int? = null
        var timestamp: Long = 0
        var title: String? = null
        var showTraktLogo: Boolean = false
        var description: String? = null
        var network: String? = null
        var posterUrl: String? = null
        var username: String? = null
        var avatar: String? = null
        var action: String? = null

        @ItemType
        var type = 0

        fun recentlyWatchedLocal(): Item {
            this.type = ItemType.HISTORY
            return this
        }

        fun recentlyWatchedTrakt(action: String?): Item {
            this.action = action
            this.type = ItemType.HISTORY
            return this
        }

        fun friend(username: String?, avatar: String?, action: String?): Item {
            this.username = username
            this.avatar = avatar
            this.action = action
            this.type = ItemType.FRIEND
            return this
        }

        /**
         * Pass 0 if no value.
         */
        fun episodeIds(episodeRowId: Long, showTmdbId: Int): Item {
            this.episodeRowId = episodeRowId
            this.showTmdbId = showTmdbId
            return this
        }

        fun tmdbId(movieTmdbId: Int?): Item {
            this.movieTmdbId = movieTmdbId
            return this
        }

        fun displayData(
            timestamp: Long, title: String?, description: String?,
            posterUrl: String?
        ): Item {
            this.timestamp = timestamp
            this.title = title
            this.description = description
            this.posterUrl = posterUrl
            return this
        }

        fun moreLink(title: String): Item {
            this.type = ItemType.MORE_LINK
            this.title = title
            return this
        }

        fun header(title: String, showTraktLogo: Boolean): Item {
            this.type = ItemType.HEADER
            this.title = title
            this.showTraktLogo = showTraktLogo
            return this
        }
    }

    protected val drawableWatched: Drawable
    protected val drawableCheckin: Drawable

    private val dataset: MutableList<Item>
    private var recentlyWatched: List<Item>? = null
    private var friendsRecently: List<Item>? = null

    init {
        dataset = ArrayList()
        drawableWatched = AppCompatResources.getDrawable(context, R.drawable.ic_watch_16dp)!!
        drawableCheckin = AppCompatResources.getDrawable(context, R.drawable.ic_checkin_16dp)!!
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.HEADER -> {
                HeaderViewHolder(viewGroup)
            }

            ViewType.MORE_LINK -> {
                MoreViewHolder(viewGroup, listener)
            }

            ViewType.HISTORY -> {
                getHistoryViewHolder(viewGroup, listener)
            }

            else -> {
                throw IllegalArgumentException("Using unrecognized view type.")
            }
        }
    }

    private fun getHistoryViewHolder(
        viewGroup: ViewGroup,
        itemClickListener: ItemClickListener
    ): RecyclerView.ViewHolder {
        return HistoryViewHolder(
            ItemHistoryBinding.inflate(
                LayoutInflater.from(viewGroup.context), viewGroup, false
            ),
            itemClickListener
        )
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (viewHolder) {
            is HeaderViewHolder -> viewHolder.bind(item.title, item.showTraktLogo)

            is MoreViewHolder -> {
                viewHolder.title.text = item.title
            }

            is HistoryViewHolder -> {
                viewHolder.bindToShow(context, item, drawableWatched, drawableCheckin)
            }
        }
    }

    override fun getItemCount(): Int = dataset.size

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            ItemType.HISTORY, ItemType.FRIEND -> ViewType.HISTORY
            ItemType.MORE_LINK -> ViewType.MORE_LINK
            ItemType.HEADER -> ViewType.HEADER
            else -> 0
        }
    }

    fun getItem(position: Int): Item = dataset[position]

    fun setRecentlyWatched(items: List<Item>?) {
        val oldCount = recentlyWatched?.size ?: 0
        val newCount = items?.size ?: 0
        recentlyWatched = items
        reloadData()
        notifyAboutChanges(0, oldCount, newCount)
    }

    fun setFriendsRecentlyWatched(items: List<Item>?) {
        val oldCount = friendsRecently?.size ?: 0
        val newCount = items?.size ?: 0
        // items start after recently watched (if any)
        val startPosition = recentlyWatched?.size ?: 0
        friendsRecently = items
        reloadData()
        notifyAboutChanges(startPosition, oldCount, newCount)
    }

    private fun reloadData() {
        dataset.clear()
        val recentlyWatched = recentlyWatched
        if (recentlyWatched != null) {
            dataset.addAll(recentlyWatched)
        }
        val friendsRecently = friendsRecently
        if (friendsRecently != null) {
            dataset.addAll(friendsRecently)
        }
    }

    private fun notifyAboutChanges(startPosition: Int, oldItemCount: Int, newItemCount: Int) {
        if (newItemCount == 0 && oldItemCount == 0) {
            return
        }
        if (newItemCount == oldItemCount) {
            // identical number of items
            notifyItemRangeChanged(startPosition, oldItemCount)
        } else if (newItemCount > oldItemCount) {
            // more items than before
            if (oldItemCount > 0) {
                notifyItemRangeChanged(startPosition, oldItemCount)
            }
            notifyItemRangeInserted(
                startPosition + oldItemCount,
                newItemCount - oldItemCount
            )
        } else {
            // less items than before
            if (newItemCount > 0) {
                notifyItemRangeChanged(startPosition, newItemCount)
            }
            notifyItemRangeRemoved(
                startPosition + newItemCount,
                oldItemCount - newItemCount
            )
        }
    }

    companion object {
        const val TRAKT_ACTION_WATCH = "watch"
    }
}