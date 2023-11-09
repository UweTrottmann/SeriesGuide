// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.history

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemHistoryBinding
import com.battlelancer.seriesguide.shows.history.NowAdapter.NowItem
import com.battlelancer.seriesguide.util.CircleTransformation
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import java.util.Date

/**
 * Binds either a show or movie history item.
 */
class HistoryViewHolder(
    private val binding: ItemHistoryBinding,
    listener: NowAdapter.ItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.constaintLayoutHistory.setOnClickListener { v: View ->
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(v, position)
            }
        }
        // Only used in history-only view.
        binding.textViewHistoryHeader.visibility = View.GONE
    }

    private fun bindCommonInfo(
        context: Context, item: NowItem,
        drawableWatched: Drawable,
        drawableCheckin: Drawable
    ) {
        val time = TimeTools.formatToLocalRelativeTime(context, Date(item.timestamp))
        if (item.type == NowAdapter.ItemType.HISTORY) {
            // user history entry
            binding.imageViewHistoryAvatar.visibility = View.GONE
            binding.textViewHistoryInfo.text = time
        } else {
            // friend history entry
            binding.imageViewHistoryAvatar.visibility = View.VISIBLE
            binding.textViewHistoryInfo.text = TextTools.dotSeparate(item.username, time)

            // user avatar
            ImageTools.loadWithPicasso(context, item.avatar)
                .transform(avatarTransform)
                .into(binding.imageViewHistoryAvatar)
        }

        // action type indicator (only if showing Trakt history)
        val typeView = binding.imageViewHistoryType
        if (NowAdapter.TRAKT_ACTION_WATCH == item.action) {
            typeView.setImageDrawable(drawableWatched)
            typeView.visibility = View.VISIBLE
        } else if (item.action != null) {
            // check-in, scrobble
            typeView.setImageDrawable(drawableCheckin)
            typeView.visibility = View.VISIBLE
        } else {
            typeView.visibility = View.GONE
        }
        // Set disabled for darker icon (non-interactive).
        typeView.isEnabled = false
    }

    fun bindToShow(
        context: Context, item: NowItem,
        drawableWatched: Drawable,
        drawableCheckin: Drawable
    ) {
        bindCommonInfo(context, item, drawableWatched, drawableCheckin)

        ImageTools.loadShowPosterUrlResizeSmallCrop(
            context, binding.imageViewHistoryPoster,
            item.posterUrl
        )

        binding.textViewHistoryShow.text = item.title
        binding.textViewHistoryEpisode.text = item.description
    }

    fun bindToMovie(
        context: Context, item: NowItem, drawableWatched: Drawable,
        drawableCheckin: Drawable
    ) {
        bindCommonInfo(context, item, drawableWatched, drawableCheckin)

        // TMDb poster (resolved on demand as Trakt does not have them)
        ImageTools.loadShowPosterUrlResizeSmallCrop(
            context, binding.imageViewHistoryPoster,
            SgPicassoRequestHandler.SCHEME_MOVIE_TMDB + "://" + item.movieTmdbId
        )

        binding.textViewHistoryShow.text = item.title
    }

    companion object {
        private val avatarTransform = CircleTransformation()
    }
}