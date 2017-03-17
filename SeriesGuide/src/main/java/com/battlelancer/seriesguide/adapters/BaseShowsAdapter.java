package com.battlelancer.seriesguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import java.util.Date;

/**
 * Base adapter for the show item layout.
 */
public abstract class BaseShowsAdapter extends CursorAdapter {

    public interface OnContextMenuClickListener {
        void onClick(View view, ShowViewHolder viewHolder);
    }

    protected final SgApp app;
    private OnContextMenuClickListener onContextMenuClickListener;
    private final int resIdStar;
    private final int resIdStarZero;

    BaseShowsAdapter(Activity activity, OnContextMenuClickListener listener) {
        super(activity, null, 0);
        this.app = SgApp.from(activity);
        this.onContextMenuClickListener = listener;

        resIdStar = Utils.resolveAttributeToResourceId(activity.getTheme(),
                R.attr.drawableStar);
        resIdStarZero = Utils.resolveAttributeToResourceId(activity.getTheme(),
                R.attr.drawableStar0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_show, parent, false);

        ShowViewHolder viewHolder = new ShowViewHolder(v, app, onContextMenuClickListener);
        v.setTag(viewHolder);

        return v;
    }

    void setFavoriteState(ImageView view, boolean isFavorite) {
        view.setImageResource(isFavorite ? resIdStar : resIdStarZero);
        view.setContentDescription(view.getContext()
                .getString(isFavorite ? R.string.context_unfavorite : R.string.context_favorite));
    }

    void setRemainingCount(TextView textView, int unwatched) {
        if (unwatched > 0) {
            textView.setText(textView.getResources()
                    .getQuantityString(R.plurals.remaining_episodes_plural, unwatched, unwatched));
        } else {
            textView.setText(null);
        }
    }

    /**
     * Builds a network + release time string for a show formatted like "Network / Tue 08:00 PM".
     */
    static String buildNetworkAndTimeString(Context context, int time, int weekday,
            String timeZone, String country, String network) {
        // network
        StringBuilder networkAndTime = new StringBuilder();
        networkAndTime.append(network);

        // time
        if (time != -1) {
            Date release = TimeTools.getShowReleaseDateTime(context,
                    TimeTools.getShowReleaseTime(time), weekday, timeZone, country, network);
            String dayString = TimeTools.formatToLocalDayOrDaily(context, release, weekday);
            String timeString = TimeTools.formatToLocalTime(context, release);
            if (networkAndTime.length() > 0) {
                networkAndTime.append(" / ");
            }
            networkAndTime.append(dayString).append(" ").append(timeString);
        }

        return networkAndTime.toString();
    }

    public static class ShowViewHolder {

        public TextView name;
        public TextView timeAndNetwork;
        public TextView episode;
        public TextView episodeTime;
        public TextView remainingCount;
        public ImageView poster;
        public ImageView favorited;
        public ImageView contextMenu;

        public int showTvdbId;
        public int episodeTvdbId;
        public boolean isFavorited;
        public boolean isHidden;

        public ShowViewHolder(View v, final SgApp app, final OnContextMenuClickListener listener) {
            name = (TextView) v.findViewById(R.id.seriesname);
            timeAndNetwork = (TextView) v.findViewById(R.id.textViewShowsTimeAndNetwork);
            episode = (TextView) v.findViewById(R.id.TextViewShowListNextEpisode);
            episodeTime = (TextView) v.findViewById(R.id.episodetime);
            remainingCount = ButterKnife.findById(v, R.id.textViewShowsRemaining);
            poster = (ImageView) v.findViewById(R.id.showposter);
            favorited = (ImageView) v.findViewById(R.id.favoritedLabel);
            contextMenu = (ImageView) v.findViewById(R.id.imageViewShowsContextMenu);

            // favorite star
            favorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    app.getShowTools().storeIsFavorite(showTvdbId, !isFavorited);
                }
            });
            // context menu
            contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onClick(v, ShowViewHolder.this);
                    }
                }
            });
        }
    }
}
