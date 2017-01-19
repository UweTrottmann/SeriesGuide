package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
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

public class MoviesDiscoverAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final DateFormat dateFormatMovieReleaseDate;
    private final String posterBaseUrl;
    @NonNull
    private final List<String> links;
    @NonNull
    private final List<Movie> movies;

    public MoviesDiscoverAdapter(Context context) {
        this.context = context;
        this.dateFormatMovieReleaseDate = DateFormat.getDateInstance(DateFormat.MEDIUM);
        this.links = new ArrayList<>();
        this.links.add("Popular");
        this.links.add("Digital releases");
        this.links.add("Disc releases");
        this.movies = new ArrayList<>();
        this.posterBaseUrl = TmdbSettings.getPosterBaseUrl(context);
    }

    @Override
    public int getItemViewType(int position) {
        int linksCount = links.size();
        if (position < linksCount) {
            return R.layout.item_discover_link;
        }
        if (position == positionHeader()) {
            return R.layout.item_grid_header;
        }
        return R.layout.item_movie;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == R.layout.item_discover_link) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_discover_link, parent, false);
            return new LinkViewHolder(itemView);
        }
        if (viewType == R.layout.item_grid_header) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_grid_header, parent, false);
            return new HeaderViewHolder(itemView);
        }
        if (viewType == R.layout.item_movie) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_movie, parent, false);
            return new MovieViewHolder(itemView);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LinkViewHolder) {
            LinkViewHolder holderActual = (LinkViewHolder) holder;
            String link = getLink(position);
            holderActual.link.setText(link);
        }
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder holderActual = (HeaderViewHolder) holder;
            holderActual.header.setText(R.string.movies_in_theatres);
        }
        if (holder instanceof MovieViewHolder) {
            MovieViewHolder holderActual = (MovieViewHolder) holder;
            Movie movie = getMovie(position);
            holderActual.title.setText(movie.title);
            if (movie.release_date != null) {
                holderActual.date.setText(dateFormatMovieReleaseDate.format(movie.release_date));
            } else {
                holderActual.date.setText("");
            }

            // poster
            // use fixed size so bitmaps can be re-used on config change
            ServiceUtils.loadWithPicasso(context, posterBaseUrl + movie.poster_path)
                    .resizeDimen(R.dimen.movie_poster_width, R.dimen.movie_poster_height)
                    .centerCrop()
                    .into(holderActual.poster);

            // set unique transition names
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holderActual.poster.setTransitionName(position + "_moviesDiscoverAdapterPoster");
            }
        }
    }

    @Override
    public int getItemCount() {
        return links.size() + 1 /* header */ + movies.size();
    }

    private String getLink(int position) {
        return links.get(position);
    }

    private Movie getMovie(int position) {
        return movies.get(position - links.size() - 1 /** header **/);
    }

    private int positionHeader() {
        return links.size();
    }

    public void updateMovies(@Nullable List<Movie> newMovies) {
        movies.clear();
        if (newMovies != null) {
            movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.textViewGridHeader) TextView header;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class LinkViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.textViewDiscoverLink) TextView link;

        public LinkViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.textViewMovieTitle) public TextView title;
        @BindView(R.id.textViewMovieDate) public TextView date;
        @BindView(R.id.imageViewMoviePoster) public ImageView poster;
        @BindView(R.id.imageViewMovieItemContextMenu) public ImageView contextMenu;

        public MovieViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
