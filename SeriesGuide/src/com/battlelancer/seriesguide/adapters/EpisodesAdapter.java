
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.battlelancer.seriesguide.WatchedBox;
import com.battlelancer.seriesguide.ui.EpisodesFragment.EpisodesQuery;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

public class EpisodesAdapter extends CursorAdapter {

    private static final int LAYOUT = R.layout.episode_row;

    private LayoutInflater mLayoutInflater;

    private OnClickListener mOnClickListener;

    private OnFlagEpisodeListener mOnFlagListener;

    public interface OnFlagEpisodeListener {
        public void onFlagEpisodeWatched(int episodeId, int episodeNumber, boolean isWatched);
    }

    public EpisodesAdapter(Context context, Cursor c, int flags, OnClickListener listener,
            OnFlagEpisodeListener flagListener) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mOnClickListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = newView(mContext, mCursor, parent);

            viewHolder = new ViewHolder();
            viewHolder.watchedBox = (WatchedBox) convertView.findViewById(R.id.watchedBoxEpisode);
            viewHolder.episodeTitle = (TextView) convertView
                    .findViewById(R.id.textViewEpisodeTitle);
            viewHolder.episodeNumber = (TextView) convertView
                    .findViewById(R.id.textViewEpisodeNumber);
            viewHolder.episodeAirdate = (TextView) convertView
                    .findViewById(R.id.textViewEpisodeAirdate);
            viewHolder.episodeAbsoluteNumber = (TextView) convertView
                    .findViewById(R.id.textViewEpisodeAbsoluteNumber);
            viewHolder.contextMenu = (ImageView) convertView
                    .findViewById(R.id.imageViewContextMenu);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // episode title
        viewHolder.episodeTitle.setText(mCursor.getString(EpisodesQuery.TITLE));

        // watched box
        viewHolder.watchedBox.setChecked(mCursor.getInt(EpisodesQuery.WATCHED) > 0);

        final int episodeId = mCursor.getInt(EpisodesQuery._ID);
        final int episodeNumber = mCursor.getInt(EpisodesQuery.NUMBER);
        viewHolder.watchedBox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ((WatchedBox) v).toggle();
                mOnFlagListener.onFlagEpisodeWatched(episodeId, episodeNumber,
                        ((WatchedBox) v).isChecked());
            }
        });
        CheatSheet.setup(viewHolder.watchedBox,
                viewHolder.watchedBox.isChecked() ? R.string.unmark_episode
                        : R.string.mark_episode);

        // number
        StringBuilder episodenumberValue = new StringBuilder(String.valueOf(episodeNumber));
        float dvdNumber = mCursor.getFloat(EpisodesQuery.DVDNUMBER);
        if (dvdNumber > 0 && dvdNumber != episodeNumber) {
            episodenumberValue.append(" (").append(dvdNumber).append(")");
        }
        viewHolder.episodeNumber.setText(episodenumberValue);

        // absolute number
        int absoluteNumber = mCursor.getInt(EpisodesQuery.ABSOLUTE_NUMBER);
        if (absoluteNumber > 0 && absoluteNumber != episodeNumber) {
            viewHolder.episodeAbsoluteNumber.setText(String.valueOf(absoluteNumber));
        } else {
            viewHolder.episodeAbsoluteNumber.setText("");
        }

        // air date
        long airtime = mCursor.getLong(EpisodesQuery.FIRSTAIREDMS);
        if (airtime != -1) {
            viewHolder.episodeAirdate.setText(Utils.formatToTimeAndDay(airtime, mContext)[2]);
        } else {
            viewHolder.episodeAirdate.setText(mContext.getString(R.string.episode_firstaired) + " "
                    + mContext.getString(R.string.unknown));
        }

        // context menu
        viewHolder.contextMenu.setOnClickListener(mOnClickListener);

        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mLayoutInflater.inflate(LAYOUT, parent, false);
    }

    static class ViewHolder {
        TextView episodeAbsoluteNumber;
        TextView episodeAirdate;
        TextView episodeNumber;
        TextView episodeTitle;
        WatchedBox watchedBox;
        ImageView contextMenu;
    }

}
