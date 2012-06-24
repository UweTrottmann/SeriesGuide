
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.Constants.ShowSorting;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.Utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

public class CheckinActivity extends BaseActivity implements LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = R.layout.checkin;

    private EditText mSearchBox;

    private SimpleCursorAdapter mAdapter;

    private String mSearchFilter;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.checkin);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.checkin));
        actionBar.setDisplayShowTitleEnabled(true);
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
            }
        });

        // setup adapter
        String[] from = new String[] {
                SeriesContract.Shows.TITLE, SeriesContract.Shows.NEXTTEXT,
                SeriesContract.Shows.AIRSTIME, SeriesContract.Shows.NETWORK,
                SeriesContract.Shows.POSTER
        };
        int[] to = new int[] {
                R.id.seriesname, R.id.TextViewShowListNextEpisode, R.id.TextViewShowListAirtime,
                R.id.TextViewShowListNetwork, R.id.showposter
        };
        int layout = R.layout.show_rowairtime;
        mAdapter = new SlowAdapter(this, layout, null, from, to, 0);

        // setup grid view
        GridView list = (GridView) findViewById(R.id.gridViewCheckinShows);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Cursor show = (Cursor) mAdapter.getItem(position);
                final String episodeId = show.getString(CheckinQuery.NEXTEPISODE);
                if (TextUtils.isEmpty(episodeId)) {
                    return;
                }

                // look up episode
                final Cursor episode = getContentResolver().query(
                        Episodes.buildEpisodeUri(episodeId), new String[] {
                                Episodes.SEASON, Episodes.NUMBER, Episodes.TITLE
                        }, null, null, null);
                if (episode != null && episode.moveToFirst()) {
                    final String episodeString = ShareUtils.onCreateShareString(
                            CheckinActivity.this, episode);

                    // display a check-in dialog
                    CheckInDialogFragment f = CheckInDialogFragment.newInstance(
                            show.getString(CheckinQuery.IMDBID), (int) id, episode.getInt(0),
                            episode.getInt(1), episodeString);
                    f.show(getSupportFragmentManager(), "checkin-dialog");

                    episode.close();
                }
            }
        });

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/Checkin");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (mSearchFilter != null) {
            baseUri = Uri.withAppendedPath(Shows.CONTENT_FILTER_URI, Uri.encode(mSearchFilter));
        } else {
            baseUri = Shows.CONTENT_URI;
        }

        return new CursorLoader(this, baseUri, CheckinQuery.PROJECTION, CheckinQuery.SELECTION,
                null, ShowSorting.UPCOMING.query());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);
    }

    private class SlowAdapter extends SimpleCursorAdapter {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        public SlowAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.seriesname);
                viewHolder.network = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNetwork);
                viewHolder.next = (TextView) convertView.findViewById(R.id.next);
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNextEpisode);
                viewHolder.episodeTime = (TextView) convertView.findViewById(R.id.episodetime);
                viewHolder.airsTime = (TextView) convertView
                        .findViewById(R.id.TextViewShowListAirtime);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.showposter);
                viewHolder.favorited = convertView.findViewById(R.id.favoritedLabel);
                viewHolder.collected = convertView.findViewById(R.id.collectedLabel);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            viewHolder.name.setText(mCursor.getString(CheckinQuery.TITLE));
            viewHolder.network.setText(mCursor.getString(CheckinQuery.NETWORK));

            boolean isFavorited = mCursor.getInt(CheckinQuery.FAVORITE) == 1;
            viewHolder.favorited.setVisibility(isFavorited ? View.VISIBLE : View.GONE);

            // next episode info
            String fieldValue = mCursor.getString(CheckinQuery.NEXTTEXT);
            if (fieldValue.length() == 0) {
                // show show status if there are currently no more
                // episodes
                int status = mCursor.getInt(CheckinQuery.STATUS);

                // Continuing == 1 and Ended == 0
                if (status == 1) {
                    viewHolder.next.setText(getString(R.string.show_isalive));
                } else if (status == 0) {
                    viewHolder.next.setText(getString(R.string.show_isnotalive));
                } else {
                    viewHolder.next.setText("");
                }
                viewHolder.episode.setText("");
                viewHolder.episodeTime.setText("");
            } else {
                viewHolder.next.setText(getString(R.string.nextepisode));
                viewHolder.episode.setText(fieldValue);
                fieldValue = mCursor.getString(CheckinQuery.NEXTAIRDATETEXT);
                viewHolder.episodeTime.setText(fieldValue);
            }

            // airday
            String[] values = Utils.parseMillisecondsToTime(mCursor.getLong(CheckinQuery.AIRSTIME),
                    mCursor.getString(CheckinQuery.AIRSDAYOFWEEK), mContext);
            viewHolder.airsTime.setText(values[1] + " " + values[0]);

            // set poster immediately, we don't care for efficient scrolling
            // here
            final String path = mCursor.getString(CheckinQuery.POSTER);
            Utils.setPosterBitmap(viewHolder.poster, path, false, null);
            // Null tag means the view has the correct data
            viewHolder.poster.setTag(null);

            return convertView;
        }
    }

    static class ViewHolder {

        public TextView name;

        public TextView network;

        public TextView next;

        public TextView episode;

        public TextView episodeTime;

        public TextView airsTime;

        public ImageView poster;

        public View favorited;

        public View collected;
    }

    interface CheckinQuery {
        String[] PROJECTION = {
                Shows._ID, Shows.TITLE, Shows.NEXTTEXT, Shows.AIRSTIME, Shows.NETWORK,
                Shows.POSTER, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTAIRDATETEXT,
                Shows.FAVORITE, Shows.IMDBID, Shows.NEXTEPISODE, Shows.HIDDEN
        };

        String SELECTION = Shows.NEXTEPISODE + "!='' AND " + Shows.HIDDEN + "=0";

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
