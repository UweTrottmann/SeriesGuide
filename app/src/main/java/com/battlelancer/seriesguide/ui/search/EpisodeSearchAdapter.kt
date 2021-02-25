package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemSearchResultBinding
import com.battlelancer.seriesguide.provider.SgEpisode2SearchResult
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TextTools

/**
 * Displays episode search results.
 */
class EpisodeSearchAdapter(
    context: Context,
    private val clickListener: OnItemClickListener
) : ArrayAdapter<SgEpisode2SearchResult>(context, 0) {

    interface OnItemClickListener {
        fun onItemClick(anchor: View, episodeId: Long)
    }

    fun setData(data: List<SgEpisode2SearchResult>?) {
        clear()
        data?.also {
            addAll(it)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder = if (convertView == null) {
            EpisodeSearchViewHolder(parent, clickListener)
        } else {
            convertView.tag as EpisodeSearchViewHolder
        }

        val item = getItem(position)
        if (item != null) {
            viewHolder.bindTo(item, context)
        }

        return viewHolder.binding.root
    }

}

class EpisodeSearchViewHolder(
    parent: ViewGroup,
    clickListener: EpisodeSearchAdapter.OnItemClickListener
) {

    val binding =
        ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    private var episodeId: Long = 0

    init {
        binding.root.tag = this
        binding.root.setOnClickListener {
            clickListener.onItemClick(it, episodeId)
        }
    }

    fun bindTo(episode: SgEpisode2SearchResult, context: Context) {
        episodeId = episode.id

        binding.textViewSearchShow.text = episode.seriestitle
        val episodeFlag: Int = episode.watched
        when {
            EpisodeTools.isWatched(episodeFlag) -> {
                binding.imageViewSearchWatched.setImageResource(R.drawable.ic_watched_24dp)
            }
            EpisodeTools.isSkipped(episodeFlag) -> {
                binding.imageViewSearchWatched.setImageResource(R.drawable.ic_skipped_24dp)
            }
            else -> {
                binding.imageViewSearchWatched.setImageResource(R.drawable.ic_watch_black_24dp)
            }
        }

        // ensure matched term is bold
        val snippet: String? = episode.overview
        binding.textViewSearchSnippet.text = if (snippet != null) Html.fromHtml(snippet) else null

        // episode
        binding.textViewSearchEpisode.text = TextTools.getNextEpisodeString(
            context,
            episode.season,
            episode.episodenumber,
            episode.episodetitle
        )

        // poster
        ImageTools.loadShowPosterResizeSmallCrop(
            context,
            binding.imageViewSearchPoster,
            episode.series_poster_small
        )
    }

}
