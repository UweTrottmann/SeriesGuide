package com.battlelancer.seriesguide.ui.search

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools

class ShowsPopularViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.textViewAddTitle)
    private val description = itemView.findViewById<TextView>(R.id.textViewAddDescription)
    private val poster = itemView.findViewById<ImageView>(R.id.imageViewAddPoster)
    private val addIndicator = itemView.findViewById<AddIndicator>(R.id.addIndicatorAddShow)
    private val buttonContextMenu = itemView.findViewById<ImageView>(R.id.buttonItemAddMore)

    fun bindTo(searchResult: SearchResult?) {
        title.text = searchResult?.title
        description.text = searchResult?.overview

        // only local shows will have a poster path set
        // try to fall back to the first uploaded TVDB poster for all others
        val posterUrl = if (searchResult != null) {
            TvdbImageTools.smallSizeOrResolveUrl(searchResult.posterPath,
                    searchResult.tvdbid, searchResult.language)
        } else null
        TvdbImageTools.loadUrlResizeCrop(itemView.context, poster, posterUrl)
    }

    companion object {
        fun create(parent: ViewGroup): ShowsPopularViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_addshow, parent, false)
            return ShowsPopularViewHolder(view)
        }


    }

}