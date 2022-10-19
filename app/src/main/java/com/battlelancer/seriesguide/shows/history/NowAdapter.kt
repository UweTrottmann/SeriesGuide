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

/**
 * Sectioned adapter displaying recently watched episodes and episodes recently watched by
 * friends.
 */
open class NowAdapter(
    protected val context: Context,
    private val listener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    internal class MoreViewHolder(itemView: View, listener: ItemClickListener) :
        RecyclerView.ViewHolder(itemView) {
        var title: TextView

        init {
            title = itemView.findViewById(R.id.textViewNowMoreText)
            itemView.setOnClickListener { v: View ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(v, position)
                }
            }
        }
    }

    internal class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView

        init {
            title = itemView.findViewById(R.id.textViewGridHeader)
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

    class NowItem {
        var episodeRowId: Long? = null
        var showTmdbId: Int? = null
        var movieTmdbId: Int? = null
        var timestamp: Long = 0
        var title: String? = null
        var description: String? = null
        var network: String? = null
        var posterUrl: String? = null
        var username: String? = null
        var avatar: String? = null
        var action: String? = null

        @ItemType
        var type = 0

        fun recentlyWatchedLocal(): NowItem {
            this.type = ItemType.HISTORY
            return this
        }

        fun recentlyWatchedTrakt(action: String?): NowItem {
            this.action = action
            this.type = ItemType.HISTORY
            return this
        }

        fun friend(username: String?, avatar: String?, action: String?): NowItem {
            this.username = username
            this.avatar = avatar
            this.action = action
            this.type = ItemType.FRIEND
            return this
        }

        /**
         * Pass 0 if no value.
         */
        fun episodeIds(episodeRowId: Long, showTmdbId: Int): NowItem {
            this.episodeRowId = episodeRowId
            this.showTmdbId = showTmdbId
            return this
        }

        fun tmdbId(movieTmdbId: Int?): NowItem {
            this.movieTmdbId = movieTmdbId
            return this
        }

        fun displayData(
            timestamp: Long, title: String?, description: String?,
            posterUrl: String?
        ): NowItem {
            this.timestamp = timestamp
            this.title = title
            this.description = description
            this.posterUrl = posterUrl
            return this
        }

        fun moreLink(title: String): NowItem {
            this.type = ItemType.MORE_LINK
            this.title = title
            return this
        }

        fun header(title: String): NowItem {
            this.type = ItemType.HEADER
            this.title = title
            return this
        }
    }

    protected val drawableWatched: Drawable
    protected val drawableCheckin: Drawable

    private val dataset: MutableList<NowItem>
    private var recentlyWatched: List<NowItem>? = null
    private var friendsRecently: List<NowItem>? = null

    init {
        dataset = ArrayList()
        drawableWatched = AppCompatResources.getDrawable(context, R.drawable.ic_watch_16dp)!!
        drawableCheckin = AppCompatResources.getDrawable(context, R.drawable.ic_checkin_16dp)!!
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.HEADER -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.item_grid_header, viewGroup, false)
                HeaderViewHolder(v)
            }
            ViewType.MORE_LINK -> {
                val v = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.item_now_more, viewGroup, false)
                MoreViewHolder(v, listener)
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
            is HeaderViewHolder -> {
                viewHolder.title.text = item.title
            }
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

    fun getItem(position: Int): NowItem = dataset[position]

    fun setRecentlyWatched(items: List<NowItem>?) {
        val oldCount = recentlyWatched?.size ?: 0
        val newCount = items?.size ?: 0
        recentlyWatched = items
        reloadData()
        notifyAboutChanges(0, oldCount, newCount)
    }

    fun setFriendsRecentlyWatched(items: List<NowItem>?) {
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