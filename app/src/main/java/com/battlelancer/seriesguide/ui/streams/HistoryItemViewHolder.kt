package com.battlelancer.seriesguide.ui.streams

import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.collection.SparseArrayCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemHistoryBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.trakt5.entities.HistoryEntry

class HistoryItemViewHolder(
    val binding: ItemHistoryBinding,
    itemClickListener: BaseHistoryAdapter.OnItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var item: HistoryEntry? = null

    init {
        binding.imageViewHistoryAvatar.isGone = true
        binding.root.setOnClickListener { view ->
            item?.let { itemClickListener.onItemClick(view, it) }
        }
    }

    fun bindCommon(
        item: HistoryEntry,
        drawableWatched: Drawable,
        drawableCheckIn: Drawable
    ) {
        this.item = item

        // action type indicator
        if ("watch" == item.action) {
            // marked watched
            binding.imageViewHistoryType.setImageDrawable(drawableWatched)
        } else {
            // check-in, scrobble
            binding.imageViewHistoryType.setImageDrawable(drawableCheckIn)
        }
        // Set disabled for darker icon (non-interactive).
        binding.imageViewHistoryType.isEnabled = false

        // timestamp
        val watchedAt = item.watched_at
        if (watchedAt != null) {
            val timestamp =
                DateUtils.getRelativeTimeSpanString(
                    watchedAt.toInstant().toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                )
            binding.textViewHistoryInfo.text = timestamp
        } else {
            binding.textViewHistoryInfo.text = null
        }
    }

    fun bindToEpisode(item: HistoryEntry, localShowPosters: SparseArrayCompat<String>?) {
        val context = binding.root.context.applicationContext

        // show title
        binding.textViewHistoryShow.text = item.show?.title
        // show poster, use a TVDB one
        val showTvdbId = item.show?.ids?.tvdb
        val posterUrl = if (localShowPosters != null && showTvdbId != null) {
            // prefer poster of already added show, fall back to first uploaded poster
            TvdbImageTools.posterUrlOrResolve(
                localShowPosters.get(showTvdbId),
                showTvdbId,
                DisplaySettings.LANGUAGE_EN
            )
        } else {
            null
        }
        TvdbImageTools.loadShowPosterResizeSmallCrop(
            context,
            binding.imageViewHistoryPoster,
            posterUrl
        )

        // episode
        val episode = item.episode
        val number = episode?.number
        val season = episode?.season
        if (episode != null && season != null && number != null) {
            binding.textViewHistoryEpisode.text =
                TextTools.getNextEpisodeString(context, season, number, episode.title)
        } else {
            binding.textViewHistoryEpisode.text = null
        }
    }

    fun bindToMovie(item: HistoryEntry) {
        // movie title
        binding.textViewHistoryShow.text = item.movie?.title

        val movieTmdbId = item.movie?.ids?.tmdb
        val posterUrl = if (movieTmdbId != null) {
            // TMDb poster (resolved on demand as trakt does not have them)
            "movietmdb://$movieTmdbId"
        } else {
            null // no poster
        }
        val context = binding.root.context.applicationContext
        TvdbImageTools.loadShowPosterResizeSmallCrop(
            context,
            binding.imageViewHistoryPoster,
            posterUrl
        )
    }

    companion object {
        fun inflate(
            parent: ViewGroup,
            itemClickListener: BaseHistoryAdapter.OnItemClickListener
        ): HistoryItemViewHolder {
            return HistoryItemViewHolder(
                ItemHistoryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                itemClickListener
            )
        }

        fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem.watched_at == newItem.watched_at
                    && oldItem.action == newItem.action
                    && oldItem.show?.title == newItem.show?.title
                    && oldItem.episode?.title == newItem.episode?.title
                    && oldItem.movie?.title == newItem.movie?.title
        }
    }

}