
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.EpisodesFragment.EpisodesQuery;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.widgets.WatchedBox;
import com.uwetrottmann.androidutils.CheatSheet;
import java.text.NumberFormat;
import java.util.Date;

public class EpisodesAdapter extends CursorAdapter {

    public interface OnFlagEpisodeListener {
        void onFlagEpisodeWatched(int episodeId, int episodeNumber, boolean isWatched);
    }

    public interface PopupMenuClickListener {
        void onPopupMenuClick(View v, int episodeTvdbId, int episodeNumber,
                long releaseTimeMs, int watchedFlag, boolean isCollected);
    }

    private PopupMenuClickListener popupMenuClickListener;
    private OnFlagEpisodeListener onFlagListener;
    private NumberFormat integerFormat;

    public EpisodesAdapter(Context context, PopupMenuClickListener listener,
            OnFlagEpisodeListener flagListener) {
        super(context, null, 0);
        popupMenuClickListener = listener;
        onFlagListener = flagListener;
        integerFormat = NumberFormat.getIntegerInstance();
    }

    /**
     * Get the item position in the data set, or the position of the first item if it is not found.
     */
    public int getItemPosition(long itemId) {
        Cursor cursor = getCursor();
        if (cursor != null) {
            int rowId = cursor.getColumnIndexOrThrow("_id");
            for (int position = 0; position < cursor.getCount(); position++) {
                if (!cursor.moveToPosition(position)) {
                    return 0;
                }
                if (cursor.getLong(rowId) == itemId) {
                    return position;
                }
            }
        }

        return 0;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // episode title
        final int watchedFlag = mCursor.getInt(EpisodesQuery.WATCHED);
        final int episodeNumber = mCursor.getInt(EpisodesQuery.NUMBER);
        boolean hideTitle = EpisodeTools.isUnwatched(watchedFlag)
                && DisplaySettings.preventSpoilers(mContext);
        viewHolder.episodeTitle.setText(TextTools.getEpisodeTitle(mContext,
                hideTitle ? null : mCursor.getString(EpisodesQuery.TITLE), episodeNumber));

        // number
        viewHolder.episodeNumber.setText(integerFormat.format(episodeNumber));

        // watched box
        viewHolder.watchedBox.setEpisodeFlag(watchedFlag);
        final int episodeId = mCursor.getInt(EpisodesQuery._ID);
        viewHolder.watchedBox.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                WatchedBox box = (WatchedBox) v;
                // disable button, will be re-enabled on data reload once action completes
                box.setEnabled(false);
                onFlagListener.onFlagEpisodeWatched(episodeId, episodeNumber,
                        !EpisodeTools.isWatched(box.getEpisodeFlag()));
            }
        });
        viewHolder.watchedBox.setEnabled(true);
        boolean watched = EpisodeTools.isWatched(watchedFlag);
        viewHolder.watchedBox.setContentDescription(
                mContext.getString(watched ? R.string.action_unwatched : R.string.action_watched));
        CheatSheet.setup(viewHolder.watchedBox,
                watched ? R.string.action_unwatched : R.string.action_watched
        );

        // collected tag
        final boolean isCollected = mCursor.getInt(EpisodesQuery.COLLECTED) == 1;
        viewHolder.collected.setVisibility(isCollected ? View.VISIBLE : View.INVISIBLE);

        // alternative numbers
        int absoluteNumber = mCursor.getInt(EpisodesQuery.ABSOLUTE_NUMBER);
        String absoluteNumberText = null;
        if (absoluteNumber > 0) {
            absoluteNumberText = NumberFormat.getIntegerInstance().format(absoluteNumber);
        }
        double dvdNumber = mCursor.getDouble(EpisodesQuery.DVDNUMBER);
        String dvdNumberText = null;
        if (dvdNumber > 0) {
            dvdNumberText = mContext.getString(R.string.episode_number_disk) + " " + dvdNumber;
        }
        viewHolder.episodeAlternativeNumbers.setText(
                TextTools.dotSeparate(absoluteNumberText, dvdNumberText));

        // release time
        boolean isReleased;
        final long releaseTime = mCursor.getLong(EpisodesQuery.FIRSTAIREDMS);
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(mContext, releaseTime);
            isReleased = TimeTools.isReleased(actualRelease);
            // "in 15 mins" or "Oct 31, 2010"
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(mContext);
            viewHolder.episodeAirdate.setText(displayExactDate ?
                    TimeTools.formatToLocalDateShort(mContext, actualRelease)
                    : TimeTools.formatToLocalRelativeTime(mContext, actualRelease));
        } else {
            viewHolder.episodeAirdate.setText(mContext
                    .getString(R.string.episode_firstaired_unknown));
            isReleased = false;
        }

        // dim text color if not released
        TextViewCompat.setTextAppearance(viewHolder.episodeTitle, isReleased
                ? R.style.TextAppearance_Subhead : R.style.TextAppearance_Subhead_Dim);
        TextViewCompat.setTextAppearance(viewHolder.episodeAirdate, isReleased
                ? R.style.TextAppearance_Body_Secondary : R.style.TextAppearance_Body_Dim);

        // context menu
        viewHolder.contextMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupMenuClickListener != null) {
                    popupMenuClickListener.onPopupMenuClick(v, episodeId, episodeNumber,
                            releaseTime, watchedFlag, isCollected);
                }
            }
        });
    }

    static class ViewHolder {
        @BindView(R.id.textViewEpisodeAlternativeNumbers) TextView episodeAlternativeNumbers;
        @BindView(R.id.textViewEpisodeAirdate) TextView episodeAirdate;
        @BindView(R.id.textViewEpisodeNumber) TextView episodeNumber;
        @BindView(R.id.textViewEpisodeTitle) TextView episodeTitle;
        @BindView(R.id.watchedBoxEpisode) WatchedBox watchedBox;
        @BindView(R.id.imageViewCollected) ImageView collected;
        @BindView(R.id.imageViewContextMenu) ImageView contextMenu;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }
    }
}
