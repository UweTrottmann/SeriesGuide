package com.battlelancer.seriesguide.ui.streams

import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.collection.SparseArrayCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemHistoryBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.streams.TraktEpisodeHistoryLoader.HistoryItem
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.entities.HistoryEntry
import java.util.Date

class HistoryItemViewHolder(
    val binding: ItemHistoryBinding,
    itemClickListener: BaseHistoryAdapter.OnItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var historyEntry: HistoryEntry? = null

    init {
        binding.imageViewHistoryAvatar.isGone = true
        binding.constaintLayoutHistory.setOnClickListener { view ->
            historyEntry?.let { itemClickListener.onItemClick(view, it) }
        }
    }

    fun bindCommon(
        item: HistoryItem,
        previousItem: HistoryItem?,
        drawableWatched: Drawable,
        drawableCheckIn: Drawable,
        isMultiColumn: Boolean
    ) {
        this.historyEntry = item.historyEntry

        // optional header
        val isShowingHeader = previousItem == null || previousItem.headerTime != item.headerTime
        if (isMultiColumn) {
            // In a multi-column layout it looks nicer if all items are inset by header height.
            binding.textViewHistoryHeader.isInvisible = !isShowingHeader
        } else {
            binding.textViewHistoryHeader.isGone = !isShowingHeader
        }
        binding.textViewHistoryHeader.text = if (isShowingHeader) {
            // display headers like "Mon in 3 days", also "today" when applicable
            val context = binding.root.context.applicationContext
            TimeTools.formatToLocalDayAndRelativeTime(context, Date(item.headerTime))
        } else {
            null
        }

        // action type indicator
        if ("watch" == item.historyEntry.action) {
            // marked watched
            binding.imageViewHistoryType.setImageDrawable(drawableWatched)
        } else {
            // check-in, scrobble
            binding.imageViewHistoryType.setImageDrawable(drawableCheckIn)
        }
        // Set disabled for darker icon (non-interactive).
        binding.imageViewHistoryType.isEnabled = false

        // timestamp
        val watchedAt = item.historyEntry.watched_at
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
        // show poster, use a TMDB one
        val showTmdbId = item.show?.ids?.tmdb
        val posterUrl = if (localShowPosters != null && showTmdbId != null) {
            // prefer poster of already added show, fall back to first uploaded poster
            ImageTools.posterUrlOrResolve(
                localShowPosters.get(showTmdbId),
                showTmdbId,
                DisplaySettings.LANGUAGE_EN,
                context
            )
        } else {
            null
        }
        ImageTools.loadShowPosterUrlResizeSmallCrop(
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
        ImageTools.loadShowPosterUrlResizeSmallCrop(
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

        fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            val old = oldItem.historyEntry
            val new = newItem.historyEntry
            return old.watched_at == new.watched_at
                    && old.action == new.action
                    && old.show?.title == new.show?.title
                    && old.episode?.title == new.episode?.title
                    && old.movie?.title == new.movie?.title
        }
    }

}