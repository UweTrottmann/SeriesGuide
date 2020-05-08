package com.battlelancer.seriesguide.ui.movies;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cursoradapter.widget.CursorAdapter;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import java.text.DateFormat;
import java.util.Date;

class MoviesCursorAdapter extends CursorAdapter {

    private final int uniqueId;
    private final String tmdbImageBaseUrl;

    private final DateFormat dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat();

    private final MovieClickListenerImpl movieClickListener;

    MoviesCursorAdapter(Context context, MovieClickListenerImpl movieClickListener, int uniqueId) {
        super(context, null, 0);
        this.movieClickListener = movieClickListener;
        this.uniqueId = uniqueId;

        // figure out which size of posters to load based on screen density
        if (DisplaySettings.isVeryHighDensityScreen(context)) {
            tmdbImageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W342;
        } else {
            tmdbImageBaseUrl = TmdbSettings.getImageBaseUrl(context)
                    + TmdbSettings.POSTER_SIZE_SPEC_W154;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // do not use parent layout params to avoid padding issues
        @SuppressLint("InflateParams") View v =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, null);
        new ViewHolder(v, movieClickListener);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.bind(context, cursor, dateFormatMovieReleaseDate, tmdbImageBaseUrl, uniqueId);
    }

    public static class ViewHolder {
        public TextView title;
        public TextView releaseDate;
        public ImageView poster;
        public View contextMenu;

        private int movieTmdbId;

        public ViewHolder(View itemView, MovieClickListenerImpl clickListener) {
            itemView.setTag(this);

            this.title = itemView.findViewById(R.id.textViewMovieTitle);
            this.releaseDate = itemView.findViewById(R.id.textViewMovieDate);
            this.poster = itemView.findViewById(R.id.imageViewMoviePoster);
            this.contextMenu = itemView.findViewById(R.id.imageViewMovieItemContextMenu);

            itemView.setOnClickListener(v -> clickListener.onClickMovie(movieTmdbId, poster));
            // context menu
            contextMenu
                    .setOnClickListener(v -> clickListener.onClickMovieMoreOptions(movieTmdbId, v));
        }

        public void bind(Context context, Cursor cursor, DateFormat dateFormat,
                String tmdbImageBaseUrl, int uniqueId) {
            this.movieTmdbId = cursor.getInt(MoviesQuery.TMDB_ID);

            // title
            title.setText(cursor.getString(MoviesQuery.TITLE));

            // release date
            long released = cursor.getLong(MoviesQuery.RELEASED_UTC_MS);
            if (released != Long.MAX_VALUE) {
                releaseDate.setText(dateFormat.format(new Date(released)));
            } else {
                releaseDate.setText("");
            }

            // load poster, cache on external storage
            String posterPath = cursor.getString(MoviesQuery.POSTER);
            // use fixed size so bitmaps can be re-used on config change
            ServiceUtils.loadWithPicasso(context, TextUtils.isEmpty(posterPath)
                    ? null : tmdbImageBaseUrl + posterPath)
                    .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                    .centerCrop()
                    .into(poster);

            // set unique transition names
            poster.setTransitionName(
                    "moviesCursorAdapterPoster_" + uniqueId + "_" + movieTmdbId);
        }
    }

    public interface MoviesQuery {

        String[] PROJECTION = { Movies._ID, Movies.TMDB_ID, Movies.TITLE, Movies.POSTER,
                Movies.RELEASED_UTC_MS };

        int ID = 0;
        int TMDB_ID = 1;
        int TITLE = 2;
        int POSTER = 3;
        int RELEASED_UTC_MS = 4;
    }
}
