package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools

class ShowsViewHolder(
    itemView: View,
    onItemClickListener: ShowsAdapter.OnItemClickListener,
    private val drawableStar: VectorDrawableCompat,
    private val drawableStarZero: VectorDrawableCompat
) : RecyclerView.ViewHolder(itemView) {

    private val name: TextView = itemView.findViewById(R.id.seriesname)
    private val timeAndNetwork: TextView = itemView.findViewById(R.id.textViewShowsTimeAndNetwork)
    private val episode: TextView = itemView.findViewById(R.id.TextViewShowListNextEpisode)
    private val episodeTime: TextView = itemView.findViewById(R.id.episodetime)
    private val remainingCount: TextView = itemView.findViewById(R.id.textViewShowsRemaining)
    private val poster: ImageView = itemView.findViewById(R.id.showposter)
    private val favorited: ImageView = itemView.findViewById(R.id.favoritedLabel)
    private val contextMenu: ImageView = itemView.findViewById(R.id.imageViewShowsContextMenu)

    var showItem: ShowsAdapter.ShowItem? = null

    init {
        // item
        itemView.setOnClickListener { view ->
            showItem?.let {
                onItemClickListener.onItemClick(view, it.showTvdbId)
            }
        }
        // favorite star
        favorited.setOnClickListener { _ ->
            showItem?.let {
                onItemClickListener.onItemFavoriteClick(it.showTvdbId, !it.isFavorite)
            }
        }
        // context menu
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

        val isFavorite = showItem!!.isFavorite
        favorited.setImageDrawable(if (isFavorite) drawableStar else drawableStarZero)
        favorited.contentDescription =
                context.getString(if (isFavorite) R.string.context_unfavorite else R.string.context_favorite)

        // set poster
        TvdbImageTools.loadShowPosterResizeCrop(context, poster, show.posterPath)
    }

    companion object {

        fun create(
            parent: ViewGroup,
            onItemClickListener: ShowsAdapter.OnItemClickListener,
            drawableStar: VectorDrawableCompat,
            drawableStarZero: VectorDrawableCompat
        ): ShowsViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_show, parent, false)
            return ShowsViewHolder(v, onItemClickListener, drawableStar, drawableStarZero)
        }
    }

}