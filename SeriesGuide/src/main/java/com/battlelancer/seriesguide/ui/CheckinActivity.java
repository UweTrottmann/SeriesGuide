/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.ShowsDistillationSettings;
import com.battlelancer.seriesguide.ui.ShowsFragment.ViewHolder;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Displays a searchable list of shows to allow quickly checking into a shows next episode.
 */
public class CheckinActivity extends BaseNavDrawerActivity implements LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = R.layout.checkin;

    private EditText mSearchBox;

    private SlowAdapter mAdapter;

    private String mSearchFilter;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.checkin);
        setupNavDrawer();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.checkin));
        actionBar.setIcon(R.drawable.ic_action_checkin);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // setup search box
        mSearchBox = (EditText) findViewById(R.id.editTextCheckinSearch);
        mSearchBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchFilter = !TextUtils.isEmpty(s) ? s.toString() : null;
                getSupportLoaderManager().restartLoader(LOADER_ID, null, CheckinActivity.this);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // setup clear button
        findViewById(R.id.imageButtonClearSearch).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchBox.setText(null);
                mSearchBox.requestFocus();
            }
        });
        mSearchBox.requestFocus();

        // setup adapter
        mAdapter = new SlowAdapter(this, null, 0);

        // setup grid view
        GridView list = (GridView) findViewById(R.id.gridViewCheckinShows);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Cursor show = (Cursor) mAdapter.getItem(position);
                int episodeTvdbId = show.getInt(CheckinQuery.NEXTEPISODE);
                if (episodeTvdbId <= 0) {
                    return;
                }

                // display a check-in dialog
                CheckInDialogFragment f = CheckInDialogFragment.newInstance(CheckinActivity.this,
                        episodeTvdbId);
                f.show(getSupportFragmentManager(), "checkin-dialog");
            }
        });
        list.setEmptyView(findViewById(R.id.empty));

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (mSearchFilter != null) {
            baseUri = Uri.withAppendedPath(Shows.CONTENT_FILTER_URI, Uri.encode(mSearchFilter));
        } else {
            baseUri = Shows.CONTENT_URI;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String fakeInAnHour = String.valueOf(Utils.getFakeCurrentTime(prefs)
                + DateUtils.HOUR_IN_MILLIS);

        return new CursorLoader(this, baseUri, CheckinQuery.PROJECTION, CheckinQuery.SELECTION,
                new String[]{
                        fakeInAnHour
                }, ShowsDistillationSettings.ShowsSortOrder.EPISODE_REVERSE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);
    }

    private class SlowAdapter extends CursorAdapter {

        private LayoutInflater mLayoutInflater;

        private final int LAYOUT = R.layout.shows_row;

        public SlowAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();

            // title
            viewHolder.name.setText(mCursor.getString(CheckinQuery.TITLE));

            // favorited label
            final boolean isFavorited = mCursor.getInt(CheckinQuery.FAVORITE) == 1;
            viewHolder.favorited.setVisibility(isFavorited ? View.VISIBLE : View.GONE);

            // next episode info
            String nextText = mCursor.getString(CheckinQuery.NEXTTEXT);
            if (TextUtils.isEmpty(nextText)) {
                // show show status if there are currently no more episodes
                int status = mCursor.getInt(CheckinQuery.STATUS);

                // Continuing == 1 and Ended == 0
                if (status == 1) {
                    viewHolder.episodeTime.setText(getString(R.string.show_isalive));
                } else if (status == 0) {
                    viewHolder.episodeTime.setText(getString(R.string.show_isnotalive));
                } else {
                    viewHolder.episodeTime.setText("");
                }
                viewHolder.episode.setText("");
            } else {
                viewHolder.episode.setText(nextText);
                nextText = mCursor.getString(CheckinQuery.NEXTAIRDATETEXT);
                viewHolder.episodeTime.setText(nextText);
            }

            // network and release day
            final String[] values = Utils.parseMillisecondsToTime(
                    cursor.getLong(CheckinQuery.AIRSTIME),
                    cursor.getString(CheckinQuery.AIRSDAYOFWEEK), context);
            // one line: 'Network | Tue 08:00 PM'
            viewHolder.timeAndNetwork.setText(cursor.getString(CheckinQuery.NETWORK) + " / "
                    + values[1] + " " + values[0]);

            // poster
            final String imagePath = cursor.getString(CheckinQuery.POSTER);
            ImageProvider.getInstance(context).loadPosterThumb(viewHolder.poster, imagePath);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = mLayoutInflater.inflate(LAYOUT, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = (TextView) v.findViewById(R.id.seriesname);
            viewHolder.timeAndNetwork = (TextView) v
                    .findViewById(R.id.textViewShowsTimeAndNetwork);
            viewHolder.episode = (TextView) v
                    .findViewById(R.id.TextViewShowListNextEpisode);
            viewHolder.episodeTime = (TextView) v.findViewById(R.id.episodetime);
            viewHolder.poster = (ImageView) v.findViewById(R.id.showposter);
            viewHolder.favorited = (ImageView) v.findViewById(R.id.favoritedLabel);
            viewHolder.favorited.setBackgroundResource(0); // remove selectable background

            v.setTag(viewHolder);

            return v;
        }
    }

    interface CheckinQuery {

        String[] PROJECTION = {
                Shows._ID, Shows.TITLE, Shows.NEXTTEXT, Shows.AIRSTIME, Shows.NETWORK,
                Shows.POSTER, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTAIRDATETEXT,
                Shows.FAVORITE, Shows.IMDBID, Shows.NEXTEPISODE, Shows.HIDDEN, Shows.NEXTAIRDATEMS
        };

        String SELECTION = Shows.NEXTEPISODE + "!='' AND " + Shows.HIDDEN + "=0 AND "
                + Shows.NEXTAIRDATEMS + "<?";

        int _ID = 0;

        int TITLE = 1;

        int NEXTTEXT = 2;

        int AIRSTIME = 3;

        int NETWORK = 4;

        int POSTER = 5;

        int AIRSDAYOFWEEK = 6;

        int STATUS = 7;

        int NEXTAIRDATETEXT = 8;

        int FAVORITE = 9;

        int IMDBID = 10;

        int NEXTEPISODE = 11;
    }
}
