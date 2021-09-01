package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.uwetrottmann.tmdb2.entities.BaseMovie;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

class MoviesDiscoverAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int VIEW_TYPE_LINK = R.layout.item_discover_link;
    static final int VIEW_TYPE_HEADER = R.layout.item_discover_header;
    static final int VIEW_TYPE_MOVIE = R.layout.item_discover_movie;

    interface ItemClickListener extends MovieClickListener {
        void onClickLink(MoviesDiscoverLink link, View anchor);
    }

    private final Context context;
    private final ItemClickListener itemClickListener;
    private final DateFormat dateFormatMovieReleaseDate;
    private final String posterBaseUrl;
    private final List<BaseMovie> movies;

    static final MoviesDiscoverLink DISCOVER_LINK_DEFAULT = MoviesDiscoverLink.IN_THEATERS;
    @NonNull private static final List<MoviesDiscoverLink> links;

    static {
        links = new ArrayList<>(3);
        links.add(MoviesDiscoverLink.POPULAR);
        links.add(MoviesDiscoverLink.DIGITAL);
        links.add(MoviesDiscoverLink.DISC);
        links.add(MoviesDiscoverLink.UPCOMING);
    }

    MoviesDiscoverAdapter(Context context, @Nullable ItemClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat();
        this.posterBaseUrl = TmdbSettings.getPosterBaseUrl(context);
        this.movies = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        int linksCount = links.size();
        if (position < linksCount) {
            return VIEW_TYPE_LINK;
        }
        if (position == positionHeader()) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_MOVIE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LINK) {
            return LinkViewHolder.inflate(parent, itemClickListener);
        }
        if (viewType == VIEW_TYPE_HEADER) {
            return HeaderViewHolder.inflate(parent);
        }
        if (viewType == VIEW_TYPE_MOVIE) {
            return MovieViewHolder.inflate(parent, itemClickListener);
        }
        throw new IllegalArgumentException("Unknown view type " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LinkViewHolder) {
            LinkViewHolder holderActual = (LinkViewHolder) holder;
            MoviesDiscoverLink link = getLink(position);
            holderActual.link = link;
            holderActual.title.setText(link.titleRes);
        } else if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder holderActual = (HeaderViewHolder) holder;
            holderActual.header.setText(DISCOVER_LINK_DEFAULT.titleRes);
        } else if (holder instanceof MovieViewHolder) {
            MovieViewHolder holderActual = (MovieViewHolder) holder;
            BaseMovie movie = getMovie(position);
            holderActual.bindTo(movie, context, dateFormatMovieReleaseDate, posterBaseUrl);
        }
    }

    void updateMovies(@Nullable List<BaseMovie> newMovies) {
        movies.clear();
        if (newMovies != null) {
            movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return links.size() + 1 /* header */ + movies.size();
    }

    private MoviesDiscoverLink getLink(int position) {
        return links.get(position);
    }

    private BaseMovie getMovie(int position) {
        return movies.get(position - links.size() - 1 /* header */);
    }

    private int positionHeader() {
        return links.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.textViewDiscoverHeader);
        }

        public static HeaderViewHolder inflate(ViewGroup parent) {
            return new HeaderViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_discover_header, parent, false)
            );
        }
    }

    static class LinkViewHolder extends RecyclerView.ViewHolder {

        MoviesDiscoverLink link;
        TextView title;

        private LinkViewHolder(View itemView, final ItemClickListener itemClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewDiscoverLink);
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onClickLink(link, LinkViewHolder.this.itemView);
                }
            });
        }

        public static LinkViewHolder inflate(
                ViewGroup parent,
                final ItemClickListener itemClickListener
        ) {
            return new LinkViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_discover_link, parent, false),
                    itemClickListener
            );
        }
    }
}
