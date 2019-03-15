package com.battlelancer.seriesguide.ui.movies

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.util.ServiceUtils
import com.squareup.picasso.Picasso
import java.text.DateFormat

internal class MovieViewHolder(
    itemView: View,
    itemClickListener: MoviesAdapter.ItemClickListener?
) : RecyclerView.ViewHolder(itemView) {

    @JvmField
    var movieTmdbId: Int = 0
    @BindView(R.id.textViewMovieTitle)
    lateinit var title: TextView
    @BindView(R.id.textViewMovieDate)
    lateinit var date: TextView
    @BindView(R.id.imageViewMoviePoster)
    lateinit var poster: ImageView
    @BindView(R.id.imageViewMovieItemContextMenu)
    lateinit var contextMenu: ImageView

    init {
        ButterKnife.bind(this, itemView)

        itemView.setOnClickListener {
            itemClickListener?.onClickMovie(movieTmdbId, poster)
        }
        contextMenu.setOnClickListener { v ->
            itemClickListener?.onClickMovieMoreOptions(movieTmdbId, v)
        }
    }

    fun bindTo(sgMovie: SgMovie?, dateFormatMovieReleaseDate: DateFormat, posterBaseUrl: String) {
        if (sgMovie == null) {
            movieTmdbId = -1
            title.text = ""
            date.text = ""
            Picasso.get().cancelRequest(poster)
            poster.setImageDrawable(null)
        } else {
            movieTmdbId = sgMovie.tmdbId
            title.text = sgMovie.title
            if (sgMovie.releasedMs != null) {
                date.text = dateFormatMovieReleaseDate.format(sgMovie.releasedMs)
            } else {
                date.text = ""
            }

            // poster
            // use fixed size so bitmaps can be re-used on config change
            val context = itemView.context.applicationContext
            ServiceUtils.loadWithPicasso(context, posterBaseUrl + sgMovie.poster)
                .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                .centerCrop()
                .into(poster)
        }
    }
}
