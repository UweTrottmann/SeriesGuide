
package com.battlelancer.seriesguide.ui.search;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.EpisodeSearchQuery;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * {@link CursorAdapter} displaying episode search results inside the {@link
 * EpisodeSearchFragment}.
 */
class EpisodeResultsAdapter extends CursorAdapter {

    EpisodeResultsAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.showTitle.setText(mCursor.getString(EpisodeSearchQuery.SHOW_TITLE));
        Resources.Theme theme = mContext.getTheme();
        int episodeFlag = mCursor.getInt(EpisodeSearchQuery.WATCHED);
        if (EpisodeTools.isWatched(episodeFlag)) {
            viewHolder.watchedStatus.setImageResource(R.drawable.ic_watched_24dp);
        } else if (EpisodeTools.isSkipped(episodeFlag)) {
            viewHolder.watchedStatus.setImageResource(R.drawable.ic_skipped_24dp);
        } else {
            ViewTools.setVectorIcon(theme, viewHolder.watchedStatus,
                    R.drawable.ic_watch_black_24dp);
        }

        // ensure matched term is bold
        String snippet = mCursor.getString(EpisodeSearchQuery.OVERVIEW);
        viewHolder.searchSnippet.setText(snippet != null ? Html.fromHtml(snippet) : null);

        // episode
        int number = mCursor.getInt(EpisodeSearchQuery.NUMBER);
        int season = mCursor.getInt(EpisodeSearchQuery.SEASON);
        String title = mCursor.getString(EpisodeSearchQuery.TITLE);
        viewHolder.episodeTitle
                .setText(TextTools.getNextEpisodeString(mContext, season, number, title));

        // poster
        TvdbImageTools.loadShowPosterResizeSmallCrop(context, viewHolder.poster,
                TvdbImageTools.smallSizeUrl(cursor.getString(EpisodeSearchQuery.SHOW_POSTER)));
    }

    static class ViewHolder {

        @BindView(R.id.textViewSearchShow) TextView showTitle;
        @BindView(R.id.textViewSearchEpisode) TextView episodeTitle;
        @BindView(R.id.textViewSearchSnippet) TextView searchSnippet;
        @BindView(R.id.imageViewSearchWatched) ImageView watchedStatus;
        @BindView(R.id.imageViewSearchPoster) ImageView poster;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }
    }
}
