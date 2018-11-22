package com.battlelancer.seriesguide.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.ui.search.AddFragment.AddAdapter.OnItemClickListener

class ShowsPopularViewHolder(itemView: View, onItemClickListener: OnItemClickListener)
    : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.textViewAddTitle)
    private val description = itemView.findViewById<TextView>(R.id.textViewAddDescription)
    private val poster = itemView.findViewById<ImageView>(R.id.imageViewAddPoster)
    private val addIndicator = itemView.findViewById<AddIndicator>(R.id.addIndicatorAddShow)
    private val buttonContextMenu = itemView.findViewById<ImageView>(R.id.buttonItemAddMore)
    private var item: SearchResult? = null

    init {
        itemView.setOnClickListener { _ ->
            item?.let {
                onItemClickListener.onItemClick(it)
            }
        }
        addIndicator.setOnAddClickListener { _ ->
            item?.let {
                onItemClickListener.onAddClick(it)
            }
        }
    }

    fun bindTo(searchResult: SearchResult?) {
        this.item = searchResult

        // hide watchlist menu
        buttonContextMenu.visibility = View.GONE

        // display added indicator instead of add button if already added that show
        val showTitle = searchResult?.title
        if (searchResult != null) {
            addIndicator.setState(searchResult.state)
            addIndicator.setContentDescriptionAdded(
                    itemView.context.getString(R.string.add_already_exists, showTitle))
            addIndicator.visibility = View.VISIBLE
        } else {
            addIndicator.visibility = View.GONE
        }

        title.text = showTitle
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
        fun create(parent: ViewGroup,
                onItemClickListener: OnItemClickListener): ShowsPopularViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_addshow, parent, false)
            return ShowsPopularViewHolder(view, onItemClickListener)
        }
    }

}