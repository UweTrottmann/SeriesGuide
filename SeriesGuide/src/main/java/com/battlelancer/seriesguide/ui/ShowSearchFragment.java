/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.BaseShowsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.ShowsDistillationSettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import de.greenrobot.event.EventBus;

/**
 * Displays show search results.
 */
public class ShowSearchFragment extends ListFragment {

    private ShowResultsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ShowResultsAdapter(getActivity(), null, 0);
        setListAdapter(adapter);

        // initially display shows with recently released episodes
        getLoaderManager().initLoader(SearchActivity.SHOWS_LOADER_ID, new Bundle(),
                searchLoaderCallbacks);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), OverviewActivity.class);
        i.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, (int) id);

        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                        .toBundle());
    }

    public void onEventMainThread(SearchActivity.SearchQueryEvent event) {
        search(event.args);
    }

    public void search(Bundle args) {
        getLoaderManager().restartLoader(SearchActivity.SHOWS_LOADER_ID, args,
                searchLoaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<Cursor>
            searchLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String query = args.getString(SearchManager.QUERY);
            if (TextUtils.isEmpty(query)) {
                // empty query selects shows with next episodes before this point in time
                String customTimeInOneHour = String.valueOf(TimeTools.getCurrentTime(getActivity())
                        + DateUtils.HOUR_IN_MILLIS);
                return new CursorLoader(getActivity(), SeriesGuideContract.Shows.CONTENT_URI,
                        SearchQuery.PROJECTION,
                        SeriesGuideContract.Shows.NEXTEPISODE + "!='' AND "
                                + SeriesGuideContract.Shows.HIDDEN + "=0 AND "
                                + SeriesGuideContract.Shows.NEXTAIRDATEMS + "<?",
                        new String[] { customTimeInOneHour },
                        ShowsDistillationSettings.ShowsSortOrder.EPISODE_REVERSE);
            } else {
                Uri uri = SeriesGuideContract.Shows.CONTENT_URI_FILTER.buildUpon()
                        .appendPath(query)
                        .build();
                return new CursorLoader(getActivity(), uri, SearchQuery.PROJECTION, null, null,
                        null);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            adapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.swapCursor(null);
        }
    };

    private static class ShowResultsAdapter extends BaseShowsAdapter {

        private final int resIdStar;
        private final int resIdStarZero;

        public ShowResultsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            resIdStar = Utils.resolveAttributeToResourceId(context.getTheme(), R.attr.drawableStar);
            resIdStarZero = Utils.resolveAttributeToResourceId(context.getTheme(),
                    R.attr.drawableStar0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ShowViewHolder viewHolder = (ShowViewHolder) view.getTag();

            viewHolder.name.setText(cursor.getString(SearchQuery.TITLE));

            viewHolder.showTvdbId = cursor.getInt(SearchQuery.ID);
            viewHolder.isFavorited = cursor.getInt(SearchQuery.FAVORITE) == 1;

            // favorited label
            viewHolder.favorited.setImageResource(
                    viewHolder.isFavorited ? resIdStar : resIdStarZero);

            // network, day and time
            viewHolder.timeAndNetwork.setText(buildNetworkAndTimeString(context,
                    cursor.getInt(SearchQuery.RELEASE_TIME),
                    cursor.getInt(SearchQuery.RELEASE_WEEKDAY),
                    cursor.getString(SearchQuery.RELEASE_TIMEZONE),
                    cursor.getString(SearchQuery.RELEASE_COUNTRY),
                    cursor.getString(SearchQuery.NETWORK)));

            // poster
            Utils.loadTvdbShowPoster(context, viewHolder.poster,
                    cursor.getString(SearchQuery.POSTER));

            // context menu
            viewHolder.isHidden = DBUtils.restoreBooleanFromInt(cursor.getInt(SearchQuery.HIDDEN));
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);

            final ShowViewHolder viewHolder = (ShowViewHolder) v.getTag();
            viewHolder.favorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowTools.get(v.getContext())
                            .storeIsFavorite(viewHolder.showTvdbId, !viewHolder.isFavorited);
                }
            });
            viewHolder.contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.shows_popup_menu);

                    // show/hide some menu items depending on show properties
                    Menu menu = popupMenu.getMenu();
                    menu.findItem(R.id.menu_action_shows_favorites_add)
                            .setVisible(!viewHolder.isFavorited);
                    menu.findItem(R.id.menu_action_shows_favorites_remove)
                            .setVisible(viewHolder.isFavorited);
                    menu.findItem(R.id.menu_action_shows_hide).setVisible(!viewHolder.isHidden);
                    menu.findItem(R.id.menu_action_shows_unhide).setVisible(viewHolder.isHidden);

                    // hide non-relevant actions
                    menu.findItem(R.id.menu_action_shows_watched_next).setVisible(false);
                    menu.findItem(R.id.menu_action_shows_manage_lists).setVisible(false);
                    menu.findItem(R.id.menu_action_shows_update).setVisible(false);
                    menu.findItem(R.id.menu_action_shows_remove).setVisible(false);

                    popupMenu.setOnMenuItemClickListener(
                            new PopupMenuItemClickListener(v.getContext(), viewHolder.showTvdbId));
                    popupMenu.show();
                }
            });

            return v;
        }

        private static class PopupMenuItemClickListener
                implements PopupMenu.OnMenuItemClickListener {

            private final int showTvdbId;
            private final Context context;

            public PopupMenuItemClickListener(Context context, int showTvdbId) {
                this.showTvdbId = showTvdbId;
                this.context = context;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_shows_favorites_add: {
                        ShowTools.get(context).storeIsFavorite(showTvdbId, true);
                        return true;
                    }
                    case R.id.menu_action_shows_favorites_remove: {
                        ShowTools.get(context).storeIsFavorite(showTvdbId, false);
                        return true;
                    }
                    case R.id.menu_action_shows_hide: {
                        ShowTools.get(context).storeIsHidden(showTvdbId, true);
                        return true;
                    }
                    case R.id.menu_action_shows_unhide: {
                        ShowTools.get(context).storeIsHidden(showTvdbId, false);
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private interface SearchQuery {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Shows._ID, // 0
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.POSTER,
                SeriesGuideContract.Shows.FAVORITE,
                SeriesGuideContract.Shows.HIDDEN, // 4
                SeriesGuideContract.Shows.RELEASE_TIME,
                SeriesGuideContract.Shows.RELEASE_WEEKDAY,
                SeriesGuideContract.Shows.RELEASE_TIMEZONE,
                SeriesGuideContract.Shows.RELEASE_COUNTRY,
                SeriesGuideContract.Shows.NETWORK // 9
        };

        int ID = 0;
        int TITLE = 1;
        int POSTER = 2;
        int FAVORITE = 3;
        int HIDDEN = 4;
        int RELEASE_TIME = 5;
        int RELEASE_WEEKDAY = 6;
        int RELEASE_TIMEZONE = 7;
        int RELEASE_COUNTRY = 8;
        int NETWORK = 9;
    }
}
