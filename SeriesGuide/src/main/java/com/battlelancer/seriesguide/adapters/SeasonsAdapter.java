
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
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeasonsFragment.SeasonsQuery;
import com.battlelancer.seriesguide.util.SeasonTools;

public class SeasonsAdapter extends CursorAdapter {

    public interface PopupMenuClickListener {
        void onPopupMenuClick(View v, int seasonTvdbId, int seasonNumber);
    }

    private PopupMenuClickListener popupMenuClickListener;

    public SeasonsAdapter(Context context, PopupMenuClickListener listener) {
        super(context, null, 0);
        popupMenuClickListener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_season, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // title
        final int seasonNumber = mCursor.getInt(SeasonsQuery.COMBINED);
        viewHolder.seasonTitle.setText(
                SeasonTools.getSeasonString(mContext, seasonNumber));

        // progress
        final int count = mCursor.getInt(SeasonsQuery.WATCHCOUNT);
        final int unairedCount = mCursor.getInt(SeasonsQuery.UNAIREDCOUNT);
        final int noairdateCount = mCursor.getInt(SeasonsQuery.NOAIRDATECOUNT);
        final int max = mCursor.getInt(SeasonsQuery.TOTALCOUNT);
        final int progress = max - count - unairedCount - noairdateCount;
        viewHolder.seasonProgressBar.setMax(max);
        viewHolder.seasonProgressBar.setProgress(progress);
        viewHolder.seasonProgress.setText(progress + "/" + max);

        // skipped label
        viewHolder.seasonSkipped
                .setVisibility(SeasonTools.hasSkippedTag(mCursor.getString(SeasonsQuery.TAGS))
                        ? View.VISIBLE : View.GONE);

        // episodes text
        String episodeCount = "";
        // add strings for unwatched episodes
        if (count == 0) {
            // make sure there are no unchecked episodes that happen
            // to have no airdate
            if (noairdateCount == 0) {
                episodeCount += mContext.getString(R.string.season_allwatched);
            } else {
                episodeCount += noairdateCount + " ";
                if (noairdateCount == 1) {
                    episodeCount += mContext.getString(R.string.oneotherepisode);
                } else {
                    episodeCount += mContext.getString(R.string.otherepisodes);
                }
            }

            TextViewCompat.setTextAppearance(viewHolder.seasonWatchCount,
                    R.style.TextAppearance_Caption_Narrow_Dim);
        } else if (count == 1) {
            episodeCount += count + " " + mContext.getString(R.string.season_onenotwatched);
            TextViewCompat.setTextAppearance(viewHolder.seasonWatchCount,
                    R.style.TextAppearance_Caption_Narrow);
        } else {
            episodeCount += count + " " + mContext.getString(R.string.season_watchcount);
            TextViewCompat.setTextAppearance(viewHolder.seasonWatchCount,
                    R.style.TextAppearance_Caption_Narrow);
        }

        // add strings for unaired episodes
        if (unairedCount > 0) {
            episodeCount += " (+" + unairedCount + " "
                    + mContext.getString(R.string.season_unaired) + ")";
        }
        viewHolder.seasonWatchCount.setText(episodeCount);

        // context menu
        final int seasonTvdbId = mCursor.getInt(SeasonsQuery._ID);
        viewHolder.contextMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupMenuClickListener != null) {
                    popupMenuClickListener.onPopupMenuClick(v, seasonTvdbId, seasonNumber);
                }
            }
        });
    }

    static class ViewHolder {

        @BindView(R.id.textViewSeasonTitle) TextView seasonTitle;
        @BindView(R.id.textViewSeasonProgress) TextView seasonProgress;
        @BindView(R.id.progressBarSeason) ProgressBar seasonProgressBar;
        @BindView(R.id.textViewSeasonWatchCount) TextView seasonWatchCount;
        @BindView(R.id.imageViewSeasonSkipped) View seasonSkipped;
        @BindView(R.id.imageViewContextMenu) ImageView contextMenu;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }
    }
}
