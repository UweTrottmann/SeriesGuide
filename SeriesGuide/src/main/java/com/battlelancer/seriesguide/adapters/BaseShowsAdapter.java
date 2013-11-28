package com.battlelancer.seriesguide.adapters;

import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Base adapter using the shows_row.xml layout with a ViewHolder.
 */
public abstract class BaseShowsAdapter extends CursorAdapter {

    protected LayoutInflater mLayoutInflater;

    private final int LAYOUT = R.layout.shows_row;

    public BaseShowsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(LAYOUT, parent, false);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.name = (TextView) v.findViewById(R.id.seriesname);
        viewHolder.timeAndNetwork = (TextView) v.findViewById(
                R.id.textViewShowsTimeAndNetwork);
        viewHolder.episode = (TextView) v.findViewById(
                R.id.TextViewShowListNextEpisode);
        viewHolder.episodeTime = (TextView) v.findViewById(R.id.episodetime);
        viewHolder.poster = (ImageView) v.findViewById(R.id.showposter);
        viewHolder.favorited = (ImageView) v.findViewById(R.id.favoritedLabel);
        viewHolder.contextMenu = (ImageView) v.findViewById(
                R.id.imageViewShowsContextMenu);

        v.setTag(viewHolder);

        return v;
    }

    public static class ViewHolder {

        public TextView name;

        public TextView timeAndNetwork;

        public TextView episode;

        public TextView episodeTime;

        public ImageView poster;

        public ImageView favorited;

        public ImageView contextMenu;

    }

}
