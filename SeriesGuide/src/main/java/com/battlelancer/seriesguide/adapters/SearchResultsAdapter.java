
package com.battlelancer.seriesguide.adapters;

import android.content.Context;
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
import com.battlelancer.seriesguide.ui.EpisodeSearchFragment.SearchQuery;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.Utils;

/**
 * {@link CursorAdapter} displaying episode search results inside the {@link
 * com.battlelancer.seriesguide.ui.EpisodeSearchFragment}.
 */
public class SearchResultsAdapter extends CursorAdapter {

    public SearchResultsAdapter(Context context) {
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

        viewHolder.showTitle.setText(mCursor.getString(SearchQuery.SHOW_TITLE));
        //noinspection ResourceType
        viewHolder.watchedStatus.setImageResource(
                EpisodeTools.isWatched(mCursor.getInt(SearchQuery.WATCHED))
                        ? Utils.resolveAttributeToResourceId(mContext.getTheme(),
                        R.attr.drawableWatched)
                        : Utils.resolveAttributeToResourceId(mContext.getTheme(),
                                R.attr.drawableWatch));

        // ensure matched term is bold
        String snippet = mCursor.getString(SearchQuery.OVERVIEW);
        viewHolder.searchSnippet.setText(snippet != null ? Html.fromHtml(snippet) : null);

        // episode
        int number = mCursor.getInt(SearchQuery.NUMBER);
        int season = mCursor.getInt(SearchQuery.SEASON);
        String title = mCursor.getString(SearchQuery.TITLE);
        viewHolder.episodeTitle
                .setText(TextTools.getNextEpisodeString(mContext, season, number, title));
    }

    static class ViewHolder {

        @BindView(R.id.textViewShowTitle) TextView showTitle;
        @BindView(R.id.textViewEpisodeTitle) TextView episodeTitle;
        @BindView(R.id.textViewSearchSnippet) TextView searchSnippet;
        @BindView(R.id.imageViewWatchedStatus) ImageView watchedStatus;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }
    }
}
