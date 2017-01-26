package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.tmdb2.entities.Movie;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays movie titles of the given {@link Movie} array.
 */
public class MoviesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ItemClickListener {
        void onClickMovie(int movieTmdbId, ImageView posterView);

        void onClickMovieMoreOptions(int movieTmdbId, View anchor);
    }

    private final Context context;
    private final String posterBaseUrl;
    private final DateFormat dateFormatMovieReleaseDate;
    private final ItemClickListener itemClickListener;
    private final List<Movie> movies;

    public MoviesAdapter(Context context, ItemClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.dateFormatMovieReleaseDate = DateFormat.getDateInstance(DateFormat.MEDIUM);
        this.movies = new ArrayList<>();
        this.posterBaseUrl = TmdbSettings.getPosterBaseUrl(context);
    }

    public void updateMovies(@Nullable List<Movie> newMovies) {
        movies.clear();
        if (newMovies != null) {
            movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discover_movie, parent, false);
        return new MovieViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MovieViewHolder) {
            MovieViewHolder actualHolder = (MovieViewHolder) holder;
            Movie movie = movies.get(position);
            actualHolder.movieTmdbId = movie.id;
            actualHolder.title.setText(movie.title);
            if (movie.release_date != null) {
                actualHolder.date.setText(dateFormatMovieReleaseDate.format(movie.release_date));
            } else {
                actualHolder.date.setText("");
            }

            // poster
            // use fixed size so bitmaps can be re-used on config change
            ServiceUtils.loadWithPicasso(context, posterBaseUrl + movie.poster_path)
                    .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                    .centerCrop()
                    .into(actualHolder.poster);

            // set unique transition names
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actualHolder.poster.setTransitionName("moviesAdapterPoster_" + movie.id);
            }
        }
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {

        int movieTmdbId;
        @BindView(R.id.textViewMovieTitle) TextView title;
        @BindView(R.id.textViewMovieDate) TextView date;
        @BindView(R.id.imageViewMoviePoster) ImageView poster;
        @BindView(R.id.imageViewMovieItemContextMenu) ImageView contextMenu;

        public MovieViewHolder(View itemView, final ItemClickListener itemClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onClickMovie(movieTmdbId, poster);
                    }
                }
            });
            contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onClickMovieMoreOptions(movieTmdbId, v);
                    }
                }
            });
        }
    }
}
