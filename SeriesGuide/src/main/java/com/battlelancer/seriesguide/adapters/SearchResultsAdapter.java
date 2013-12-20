
package com.battlelancer.seriesguide.adapters;

import com.battlelancer.seriesguide.ui.SearchFragment;
import com.battlelancer.seriesguide.ui.SearchFragment.SearchQuery;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link CursorAdapter} displaying episode search results inside the {@link SearchFragment}.
 */
public class SearchResultsAdapter extends CursorAdapter {

    private static final int LAYOUT = R.layout.search_row;

    private LayoutInflater mLayoutInflater;

    public SearchResultsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            viewHolder.showTitle = (TextView) convertView.findViewById(R.id.textViewShowTitle);
            viewHolder.episodeTitle = (TextView) convertView
                    .findViewById(R.id.textViewEpisodeTitle);
            viewHolder.searchSnippet = (TextView) convertView
                    .findViewById(R.id.textViewSearchSnippet);
            viewHolder.watchedStatus = (ImageView) convertView
                    .findViewById(R.id.imageViewWatchedStatus);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.showTitle.setText(mCursor.getString(SearchQuery.SHOW_TITLE));
        viewHolder.watchedStatus.setImageResource(
                EpisodeTools.isWatched(mCursor.getInt(SearchQuery.WATCHED))
                        ? R.drawable.ic_ticked
                        : Utils.resolveAttributeToResourceId(mContext.getTheme(),
                                R.attr.drawableWatch));

        // ensure matched term is bold
        viewHolder.searchSnippet.setText(Html.fromHtml(mCursor.getString(SearchQuery.OVERVIEW)));

        // episode
        int number = mCursor.getInt(SearchQuery.NUMBER);
        int season = mCursor.getInt(SearchQuery.SEASON);
        String title = mCursor.getString(SearchQuery.TITLE);
        viewHolder.episodeTitle
                .setText(Utils.getNextEpisodeString(mContext, season, number, title));

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

        TextView showTitle;

        TextView episodeTitle;

        TextView searchSnippet;

        ImageView watchedStatus;
    }
}
