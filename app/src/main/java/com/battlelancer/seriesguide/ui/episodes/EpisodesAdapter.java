
package com.battlelancer.seriesguide.ui.episodes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ItemEpisodeBinding;
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

    interface ClickListener {
        void onWatchedBoxClick(long episodeId, boolean isWatched);
        void onPopupMenuClick(@NonNull View v, long episodeId, int episodeNumber,
                long releaseTimeMs, int watchedFlag, boolean isCollected);
    }

    private final ClickListener clickListener;
    private final NumberFormat integerFormat;

    EpisodesAdapter(Context context, ClickListener listener) {
        super(context, 0);
        clickListener = listener;
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
        // AbsListView tries to confirm checked positions regardless if count is 0, so check.
        if (position > getCount() - 1) {
            return -1;
        }
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
            viewHolder = new ViewHolder(parent, clickListener, integerFormat);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SgEpisode2Info item = getItem(position);
        if (item != null) {
            viewHolder.bindTo(item, getContext());
        }

        return viewHolder.binding.getRoot();
    }

    static class ViewHolder {
        private final ItemEpisodeBinding binding;
        private final ClickListener clickListener;
        private final NumberFormat integerFormat;

        public ViewHolder(ViewGroup parent,
                ClickListener clickListener,
                NumberFormat integerFormat) {
            this.clickListener = clickListener;
            this.integerFormat = integerFormat;
            this.binding = ItemEpisodeBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false);
            this.binding.getRoot().setTag(this);
        }

        public void bindTo(SgEpisode2Info episode, Context context) {
            // episode title
            final int watchedFlag = episode.getWatched();
            final int episodeNumber = episode.getEpisodenumber();
            boolean hideTitle = EpisodeTools.isUnwatched(watchedFlag)
                    && DisplaySettings.preventSpoilers(context);
            binding.textViewEpisodeTitle.setText(TextTools.getEpisodeTitle(context,
                    hideTitle ? null : episode.getTitle(), episodeNumber));

            // number
            binding.textViewEpisodeNumber.setText(integerFormat.format(episodeNumber));

            // watched box
            binding.watchedBoxEpisode.setEpisodeFlag(watchedFlag);
            binding.watchedBoxEpisode.setOnClickListener(v -> {
                WatchedBox box = (WatchedBox) v;
                // disable button, will be re-enabled on data reload once action completes
                box.setEnabled(false);
                clickListener.onWatchedBoxClick(episode.getId(),
                        !EpisodeTools.isWatched(box.getEpisodeFlag()));
            });
            binding.watchedBoxEpisode.setEnabled(true);
            boolean watched = EpisodeTools.isWatched(watchedFlag);
            binding.watchedBoxEpisode.setContentDescription(
                    context.getString(watched ? R.string.action_unwatched : R.string.action_watched));
            CheatSheet.setup(binding.watchedBoxEpisode,
                    watched ? R.string.action_unwatched : R.string.action_watched
            );

            // collected tag
            final boolean isCollected = episode.getCollected();
            binding.imageViewCollected.setVisibility(isCollected ? View.VISIBLE : View.INVISIBLE);

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
            binding.textViewEpisodeAlternativeNumbers.setText(
                    TextTools.dotSeparate(absoluteNumberText, dvdNumberText));

            // release time
            boolean isReleased;
            final long releaseTime = episode.getFirstReleasedMs();
            if (releaseTime != -1) {
                Date actualRelease = TimeTools.applyUserOffset(context, releaseTime);
                isReleased = TimeTools.isReleased(actualRelease);
                // "in 15 mins" or "Oct 31, 2010"
                boolean displayExactDate = DisplaySettings.isDisplayExactDate(context);
                binding.textViewEpisodeAirdate.setText(displayExactDate ?
                        TimeTools.formatToLocalDateShort(context, actualRelease)
                        : TimeTools.formatToLocalRelativeTime(context, actualRelease));
            } else {
                binding.textViewEpisodeAirdate.setText(context
                        .getString(R.string.episode_firstaired_unknown));
                isReleased = false;
            }

            // dim text color if not released
            TextViewCompat.setTextAppearance(binding.textViewEpisodeTitle, isReleased
                    ? R.style.TextAppearance_SeriesGuide_Subtitle1
                    : R.style.TextAppearance_SeriesGuide_Subtitle1_Dim);
            TextViewCompat.setTextAppearance(binding.textViewEpisodeAirdate, isReleased
                    ? R.style.TextAppearance_SeriesGuide_Body2_Secondary
                    : R.style.TextAppearance_SeriesGuide_Body2_Dim);

            // context menu
            this.binding.imageViewContextMenu.setOnClickListener(v -> clickListener
                    .onPopupMenuClick(v, episode.getId(), episodeNumber, releaseTime, watchedFlag,
                            isCollected));
        }
    }
}
