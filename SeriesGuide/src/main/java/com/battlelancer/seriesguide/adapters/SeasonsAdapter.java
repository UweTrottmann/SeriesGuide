
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
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
import com.uwetrottmann.androidutils.AndroidUtils;

public class SeasonsAdapter extends CursorAdapter {

    public interface PopupMenuClickListener {
        void onPopupMenuClick(View v, int seasonTvdbId, int seasonNumber);
    }

    private final PopupMenuClickListener popupMenuClickListener;
    private final boolean isRtlLayout;

    public SeasonsAdapter(Context context, PopupMenuClickListener listener) {
        super(context, null, 0);
        popupMenuClickListener = listener;
        isRtlLayout = AndroidUtils.isRtlLayout();
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

        // unwatched episodes by type
        final int released = mCursor.getInt(SeasonsQuery.WATCHCOUNT);
        final int notReleased = mCursor.getInt(SeasonsQuery.UNAIREDCOUNT);
        final int noReleaseDate = mCursor.getInt(SeasonsQuery.NOAIRDATECOUNT);

        // progress
        final int max = mCursor.getInt(SeasonsQuery.TOTALCOUNT);
        final int progress = max - released - notReleased - noReleaseDate;
        viewHolder.seasonProgressBar.setMax(max);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            viewHolder.seasonProgressBar.setProgress(progress, true);
        } else {
            viewHolder.seasonProgressBar.setProgress(progress);
        }
        Resources res = mContext.getResources();
        String textProgress;
        if (isRtlLayout) {
            textProgress = res.getString(R.string.format_progress_and_total, max, progress);
        } else {
            textProgress = res.getString(R.string.format_progress_and_total, progress, max);
        }
        viewHolder.seasonProgress.setText(textProgress);

        // skipped label
        viewHolder.seasonSkipped
                .setVisibility(SeasonTools.hasSkippedTag(mCursor.getString(SeasonsQuery.TAGS))
                        ? View.VISIBLE : View.GONE);

        // season episodes text
        StringBuilder countText = new StringBuilder();
        int watchable = released + noReleaseDate;
        if (watchable > 0) {
            // some released or other episodes left to watch
            TextViewCompat.setTextAppearance(viewHolder.seasonWatchCount,
                    R.style.TextAppearance_Caption_Narrow);
            if (released > 0) {
                countText.append(
                        res.getQuantityString(R.plurals.remaining_episodes_plural, released,
                                released));
            }
        } else {
            TextViewCompat.setTextAppearance(viewHolder.seasonWatchCount,
                    R.style.TextAppearance_Caption_Narrow_Dim);
            // ensure at least 1 watched episode by comparing amount of unwatched to total
            if (notReleased + noReleaseDate != max) {
                // all watched
                countText.append(mContext.getString(R.string.season_allwatched));
            }
        }
        if (noReleaseDate > 0) {
            // there are unwatched episodes without a release date
            if (countText.length() > 0) {
                countText.append(" · ");
            }
            countText.append(res.getQuantityString(R.plurals.other_episodes_plural,
                    noReleaseDate, noReleaseDate));
        }
        if (notReleased > 0) {
            // there are not yet released episodes
            if (countText.length() > 0) {
                countText.append(" · ");
            }
            countText.append(res.getQuantityString(R.plurals.not_released_episodes_plural,
                    notReleased, notReleased));
        }

        viewHolder.seasonWatchCount.setText(countText);

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
