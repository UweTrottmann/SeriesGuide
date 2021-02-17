package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgShow2ForLists
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools

/**
 * Display show search results.
 */
class ShowSearchAdapter(
    context: Context,
    private val listener: OnItemClickListener
) : ArrayAdapter<SgShow2ForLists>(context, 0) {

    interface OnItemClickListener {
        fun onItemClick(anchor: View, viewHolder: ShowViewHolder)
        fun onMenuClick(anchor: View, viewHolder: ShowViewHolder)
        fun onFavoriteClick(showId: Long, isFavorite: Boolean)
    }

    private val drawableStar = VectorDrawableCompat
        .create(context.resources, R.drawable.ic_star_black_24dp, context.theme)
    private val drawableStarZero = VectorDrawableCompat
        .create(context.resources, R.drawable.ic_star_border_black_24dp, context.theme)

    fun setData(data: List<SgShow2ForLists>?) {
        clear()
        data?.also {
            addAll(it)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_show, parent, false)
        val viewHolder = if (convertView == null) {
            ShowViewHolder(view, listener, drawableStar, drawableStarZero).also {
                view.tag = it
            }
        } else {
            convertView.tag as ShowViewHolder
        }

        val item: SgShow2ForLists? = getItem(position)
        if (item != null) {
            viewHolder.bindTo(item, context)
        }

        return view
    }

    class ShowViewHolder(
        v: View,
        onItemClickListener: OnItemClickListener,
        private val drawableStar: Drawable?,
        private val drawableStarZero: Drawable?
    ) {
        private val name: TextView = v.findViewById(R.id.seriesname)
        private val timeAndNetwork: TextView = v.findViewById(R.id.textViewShowsTimeAndNetwork)
        private val episode: TextView = v.findViewById(R.id.TextViewShowListNextEpisode)
        private val episodeTime: TextView = v.findViewById(R.id.episodetime)
        private val remainingCount: TextView = v.findViewById(R.id.textViewShowsRemaining)
        private val poster: ImageView = v.findViewById(R.id.showposter)
        private val favorited: ImageView = v.findViewById(R.id.favoritedLabel)
        private val contextMenu: ImageView = v.findViewById(R.id.imageViewShowsContextMenu)
        var showId = 0L
        var isFavorited = false
        var isHidden = false

        init {
            // item
            v.setOnClickListener { view: View ->
                onItemClickListener.onItemClick(view, this)
            }
            // favorite star
            favorited.setOnClickListener {
                onItemClickListener.onFavoriteClick(showId, !isFavorited)
            }
            // context menu
            contextMenu.setOnClickListener { view: View ->
                onItemClickListener.onMenuClick(view, this@ShowViewHolder)
            }
        }

        fun bindTo(show: SgShow2ForLists, context: Context) {
            showId = show.id
            isFavorited = show.favorite

            // show title
            name.text = show.title

            // favorited label
            setFavoriteState(favorited, isFavorited)

            // network, day and time
            val time = show.releaseTime
            val weekDay = show.releaseWeekDay
            val network = show.network
            val showReleaseTime = if (time != -1) {
                TimeTools.getShowReleaseDateTime(
                    context,
                    time,
                    weekDay,
                    show.releaseTimeZone,
                    show.releaseCountry,
                    network
                )
            } else {
                null
            }
            timeAndNetwork.text =
                TextTools.networkAndTime(context, showReleaseTime, weekDay, network)
            remainingCount.visibility = View.GONE // unused

            // poster
            ImageTools.loadShowPosterResizeCrop(context, poster, show.posterSmall)

            // context menu
            isHidden = show.hidden
        }

        private fun setFavoriteState(view: ImageView, isFavorite: Boolean) {
            view.setImageDrawable(if (isFavorite) drawableStar else drawableStarZero)
            view.contentDescription =
                view.context.getString(if (isFavorite) R.string.context_unfavorite else R.string.context_favorite)
        }
    }

}