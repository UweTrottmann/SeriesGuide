
package com.battlelancer.seriesguide.ui.episodes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SgEpisode2Info;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.widgets.WatchedBox;
import com.uwetrottmann.androidutils.CheatSheet;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;

class EpisodesAdapter extends ArrayAdapter<SgEpisode2Info> {

    interface OnFlagEpisodeListener {
        void onFlagEpisodeWatched(int episodeId, int episodeNumber, boolean isWatched);
    }

    interface PopupMenuClickListener {
        void onPopupMenuClick(View v, int episodeTvdbId, int episodeNumber,
                long releaseTimeMs, int watchedFlag, boolean isCollected);
    }

    private final PopupMenuClickListener popupMenuClickListener;
    private final OnFlagEpisodeListener onFlagListener;
    private final NumberFormat integerFormat;

    EpisodesAdapter(Context context, PopupMenuClickListener listener,
            OnFlagEpisodeListener flagListener) {
        super(context, 0);
        popupMenuClickListener = listener;
        onFlagListener = flagListener;
        integerFormat = NumberFormat.getIntegerInstance();
    }

    void setData(List<SgEpisode2Info> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Use episode ID instead of position to ensure Adapter.getItemIdAtPosition is stable.
     */
    @Override
    public long getItemId(int position) {
        SgEpisode2Info item = getItem(position);
        if (item != null) {
            return item.getId();
        } else {
            return -1;
        }
    }

    /**
     * Get the item position in the data set, or the position of the first item if it is not found.
     */
    int getItemPosition(long itemId) {
        for (int position = 0; position < getCount(); position++) {
            SgEpisode2Info item = getItem(position);
            if (item == null) {
                return 0;
            }
            if (item.getId() == itemId) {
                return position;
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_episode, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SgEpisode2Info item = getItem(position);
        if (item != null) {
            bindView(viewHolder, item, getContext());
        }

        return convertView;
    }

    public void bindView(ViewHolder viewHolder, SgEpisode2Info episode, Context context) {
        // episode title
        final int watchedFlag = episode.getWatched();
        final int episodeNumber = episode.getEpisodenumber();
        boolean hideTitle = EpisodeTools.isUnwatched(watchedFlag)
                && DisplaySettings.preventSpoilers(context);
        viewHolder.episodeTitle.setText(TextTools.getEpisodeTitle(context,
                hideTitle ? null : episode.getTitle(), episodeNumber));

        // number
        viewHolder.episodeNumber.setText(integerFormat.format(episodeNumber));

        // watched box
        viewHolder.watchedBox.setEpisodeFlag(watchedFlag);
        final int episodeTvdbId = episode.getEpisodeTvdbId();
        viewHolder.watchedBox.setOnClickListener(v -> {
            WatchedBox box = (WatchedBox) v;
            // disable button, will be re-enabled on data reload once action completes
            box.setEnabled(false);
            onFlagListener.onFlagEpisodeWatched(episodeTvdbId, episodeNumber,
                    !EpisodeTools.isWatched(box.getEpisodeFlag()));
        });
        viewHolder.watchedBox.setEnabled(true);
        boolean watched = EpisodeTools.isWatched(watchedFlag);
        viewHolder.watchedBox.setContentDescription(
                context.getString(watched ? R.string.action_unwatched : R.string.action_watched));
        CheatSheet.setup(viewHolder.watchedBox,
                watched ? R.string.action_unwatched : R.string.action_watched
        );

        // collected tag
        final boolean isCollected = episode.getCollected();
        viewHolder.collected.setVisibility(isCollected ? View.VISIBLE : View.INVISIBLE);

        // alternative numbers
        int absoluteNumber = episode.getAbsoluteNumber();
        String absoluteNumberText = null;
        if (absoluteNumber > 0) {
            absoluteNumberText = NumberFormat.getIntegerInstance().format(absoluteNumber);
        }
        double dvdNumber = episode.getDvdNumber();
        String dvdNumberText = null;
        if (dvdNumber > 0) {
            dvdNumberText = context.getString(R.string.episode_number_disk) + " " + dvdNumber;
        }
        viewHolder.episodeAlternativeNumbers.setText(
                TextTools.dotSeparate(absoluteNumberText, dvdNumberText));

        // release time
        boolean isReleased;
        final long releaseTime = episode.getFirstReleasedMs();
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(context, releaseTime);
            isReleased = TimeTools.isReleased(actualRelease);
            // "in 15 mins" or "Oct 31, 2010"
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
            viewHolder.episodeAirdate.setText(displayExactDate ?
                    TimeTools.formatToLocalDateShort(context, actualRelease)
                    : TimeTools.formatToLocalRelativeTime(context, actualRelease));
        } else {
            viewHolder.episodeAirdate.setText(context
                    .getString(R.string.episode_firstaired_unknown));
            isReleased = false;
        }

        // dim text color if not released
        TextViewCompat.setTextAppearance(viewHolder.episodeTitle, isReleased
                ? R.style.TextAppearance_SeriesGuide_Subtitle1
                : R.style.TextAppearance_SeriesGuide_Subtitle1_Dim);
        TextViewCompat.setTextAppearance(viewHolder.episodeAirdate, isReleased
                ? R.style.TextAppearance_SeriesGuide_Body2_Secondary
                : R.style.TextAppearance_SeriesGuide_Body2_Dim);

        // context menu
        viewHolder.contextMenu.setOnClickListener(v -> {
            if (popupMenuClickListener != null) {
                popupMenuClickListener.onPopupMenuClick(v, episodeTvdbId, episodeNumber,
                        releaseTime, watchedFlag, isCollected);
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
