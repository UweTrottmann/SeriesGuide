package com.battlelancer.seriesguide.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * Base adapter for the show item layout.
 */
public abstract class BaseShowsAdapter extends CursorAdapter {

    public interface OnItemClickListener {
        void onClick(View view, ShowViewHolder viewHolder);

        void onFavoriteClick(int showTvdbId, boolean isFavorite);
    }

    protected OnItemClickListener onItemClickListener;
    private final VectorDrawableCompat drawableStar;
    private final VectorDrawableCompat drawableStarZero;

    BaseShowsAdapter(Activity activity, OnItemClickListener listener) {
        super(activity, null, 0);
        this.onItemClickListener = listener;

        Resources.Theme theme = activity.getTheme();
        drawableStar = ViewTools.createVectorIcon(activity, theme,
                R.drawable.ic_star_black_24dp);
        drawableStarZero = ViewTools.createVectorIcon(activity, theme,
                R.drawable.ic_star_border_black_24dp);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_show, parent, false);

        ShowViewHolder viewHolder = new ShowViewHolder(v, onItemClickListener);
        v.setTag(viewHolder);

        return v;
    }

    void setFavoriteState(ImageView view, boolean isFavorite) {
        view.setImageDrawable(isFavorite ? drawableStar : drawableStarZero);
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

        private OnItemClickListener clickListener;

        public ShowViewHolder(View v, OnItemClickListener onItemClickListener) {
            name = (TextView) v.findViewById(R.id.seriesname);
            timeAndNetwork = (TextView) v.findViewById(R.id.textViewShowsTimeAndNetwork);
            episode = (TextView) v.findViewById(R.id.TextViewShowListNextEpisode);
            episodeTime = (TextView) v.findViewById(R.id.episodetime);
            remainingCount = ButterKnife.findById(v, R.id.textViewShowsRemaining);
            poster = (ImageView) v.findViewById(R.id.showposter);
            favorited = (ImageView) v.findViewById(R.id.favoritedLabel);
            contextMenu = (ImageView) v.findViewById(R.id.imageViewShowsContextMenu);
            clickListener = onItemClickListener;

            // favorite star
            favorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onFavoriteClick(showTvdbId, !isFavorited);
                    }
                }
            });
            // context menu
            contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onClick(v, ShowViewHolder.this);
                    }
                }
            });
        }
    }
}
