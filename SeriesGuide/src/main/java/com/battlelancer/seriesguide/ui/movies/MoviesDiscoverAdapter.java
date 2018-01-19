package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.tmdb2.entities.Movie;
import java.util.ArrayList;
import java.util.List;

class MoviesDiscoverAdapter extends MoviesAdapter {

    static final int VIEW_TYPE_LINK = R.layout.item_discover_link;
    static final int VIEW_TYPE_HEADER = R.layout.item_discover_header;
    static final int VIEW_TYPE_MOVIE = R.layout.item_discover_movie;

    interface ItemClickListener extends MoviesAdapter.ItemClickListener {
        void onClickLink(MoviesDiscoverLink link, View anchor);
    }

    static final MoviesDiscoverLink DISCOVER_LINK_DEFAULT = MoviesDiscoverLink.IN_THEATERS;
    @NonNull private static final List<MoviesDiscoverLink> links;

    static {
        links = new ArrayList<>(3);
        links.add(MoviesDiscoverLink.POPULAR);
        links.add(MoviesDiscoverLink.DIGITAL);
        links.add(MoviesDiscoverLink.DISC);
    }

    private final ItemClickListener itemClickListener;

    MoviesDiscoverAdapter(Context context, @Nullable ItemClickListener itemClickListener) {
        super(context, null);
        this.itemClickListener = itemClickListener;
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LINK) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(VIEW_TYPE_LINK, parent, false);
            return new LinkViewHolder(itemView, itemClickListener);
        }
        if (viewType == VIEW_TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(VIEW_TYPE_HEADER, parent, false);
            return new HeaderViewHolder(itemView);
        }
        if (viewType == VIEW_TYPE_MOVIE) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(VIEW_TYPE_MOVIE, parent, false);
            return new MovieViewHolder(itemView, itemClickListener);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LinkViewHolder) {
            LinkViewHolder holderActual = (LinkViewHolder) holder;
            MoviesDiscoverLink link = getLink(position);
            holderActual.link = link;
            holderActual.title.setText(link.titleRes);
        } else if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder holderActual = (HeaderViewHolder) holder;
            holderActual.header.setText(DISCOVER_LINK_DEFAULT.titleRes);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return links.size() + 1 /* header */ + movies.size();
    }

    private MoviesDiscoverLink getLink(int position) {
        return links.get(position);
    }

    @Override
    Movie getMovie(int position) {
        return movies.get(position - links.size() - 1 /* header */);
    }

    @NonNull
    @Override
    String getTransitionNamePrefix() {
        return "moviesDiscoverAdapterPoster_";
    }

    private int positionHeader() {
        return links.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.textViewGridHeader) TextView header;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class LinkViewHolder extends RecyclerView.ViewHolder {

        MoviesDiscoverLink link;
        @BindView(R.id.textViewDiscoverLink) TextView title;

        public LinkViewHolder(View itemView, final ItemClickListener itemClickListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onClickLink(link, LinkViewHolder.this.itemView);
                    }
                }
            });
        }
    }
}
