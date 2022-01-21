package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ImageTools

class ShowsViewHolder(
    itemView: View,
    onItemClickListener: ShowsAdapter.OnItemClickListener
) : RecyclerView.ViewHolder(itemView) {

    private val name: TextView = itemView.findViewById(R.id.seriesname)
    private val timeAndNetwork: TextView = itemView.findViewById(R.id.textViewShowsTimeAndNetwork)
    private val episode: TextView = itemView.findViewById(R.id.TextViewShowListNextEpisode)
    private val episodeTime: TextView = itemView.findViewById(R.id.episodetime)
    private val remainingCount: TextView = itemView.findViewById(R.id.textViewShowsRemaining)
    private val poster: ImageView = itemView.findViewById(R.id.showposter)
    private val favorited: ImageView = itemView.findViewById(R.id.favoritedLabel)
    private val setWatchedButton: ImageView = itemView.findViewById(R.id.imageViewShowsSetWatched)
    private val contextMenu: ImageView = itemView.findViewById(R.id.imageViewShowsContextMenu)

    private var showItem: ShowsAdapter.ShowItem? = null

    init {
        // item
        itemView.setOnClickListener { view ->
            showItem?.let {
                onItemClickListener.onItemClick(view, it.rowId)
            }
        }
        // set watched button
        TooltipCompat.setTooltipText(setWatchedButton, setWatchedButton.contentDescription)
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        setWatchedButton.setOnClickListener { v ->
            showItem?.let {
                onItemClickListener.onItemSetWatchedClick(it)
            }
        }
        // context menu
        TooltipCompat.setTooltipText(contextMenu, contextMenu.contentDescription)
        contextMenu.setOnClickListener { v ->
            showItem?.let {
                onItemClickListener.onItemMenuClick(v, it)
            }
        }
    }

    fun bind(show: ShowsAdapter.ShowItem, context: Context) {
        showItem = show

        name.text = show.name
        timeAndNetwork.text = show.timeAndNetwork
        episode.text = show.episode
        episodeTime.text = show.episodeTime

        remainingCount.text = show.remainingCount
        remainingCount.visibility = if (show.remainingCount != null) View.VISIBLE else View.GONE

        favorited.visibility = if (show.isFavorite) View.VISIBLE else View.GONE

        setWatchedButton.visibility = if (show.hasNextEpisode) View.VISIBLE else View.GONE

        // set poster
        ImageTools.loadShowPosterResizeCrop(context, poster, show.posterPath)
    }

    companion object {

        fun create(
            parent: ViewGroup,
            onItemClickListener: ShowsAdapter.OnItemClickListener
        ): ShowsViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_show_list, parent, false)
            return ShowsViewHolder(v, onItemClickListener)
        }
    }

}