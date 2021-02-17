package com.battlelancer.seriesguide.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.search.AddFragment.AddAdapter.OnItemClickListener
import com.battlelancer.seriesguide.util.ImageTools

class SearchResultViewHolder(
    itemView: View,
    onItemClickListener: OnItemClickListener
) : RecyclerView.ViewHolder(itemView) {

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
        // try to fall back to the TMDB poster for all others
        val posterUrl = if (searchResult != null) {
            ImageTools.posterUrlOrResolve(
                searchResult.posterPath,
                searchResult.tmdbId,
                searchResult.language,
                itemView.context
            )
        } else null
        ImageTools.loadShowPosterUrlResizeCrop(itemView.context, poster, posterUrl)
    }

    companion object {
        fun create(parent: ViewGroup,
                onItemClickListener: OnItemClickListener): SearchResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_addshow, parent, false)
            return SearchResultViewHolder(view, onItemClickListener)
        }
    }

}