
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Shout;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TraktShoutsFragment extends SherlockListFragment implements
        LoaderCallbacks<List<Shout>> {

    /**
     * Build a {@link TraktShoutsFragment} for shouts of an episode.
     * 
     * @param tvdbId
     * @param season
     * @param episode
     * @return
     */
    public static TraktShoutsFragment newInstance(int tvdbId, int season, int episode) {
        TraktShoutsFragment f = new TraktShoutsFragment();
        Bundle args = new Bundle();
        args.putInt(ShareItems.TVDBID, tvdbId);
        args.putInt(ShareItems.SEASON, season);
        args.putInt(ShareItems.EPISODE, episode);
        f.setArguments(args);
        return f;
    }

    /**
     * Build a {@link TraktShoutsFragment} for shouts of a show.
     * 
     * @param tvdbId
     * @return
     */
    public static TraktShoutsFragment newInstance(int tvdbId) {
        TraktShoutsFragment f = new TraktShoutsFragment();
        Bundle args = new Bundle();
        args.putInt(ShareItems.TVDBID, tvdbId);
        args.putInt(ShareItems.SEASON, -1);
        args.putInt(ShareItems.EPISODE, -1);
        f.setArguments(args);
        return f;
    }

    private TraktShoutsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.shouts_fragment, container, false);
        initializeViews(v);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new TraktShoutsAdapter(getActivity());
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(0, getArguments(), this);
    }

    private void initializeViews(View v) {
        final EditText shouttext = (EditText) v.findViewById(R.id.shouttext);
        final CheckBox checkIsSpoiler = (CheckBox) v.findViewById(R.id.checkIsSpoiler);

        v.findViewById(R.id.shoutbutton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final Bundle args = getArguments();
                int tvdbid = args.getInt(ShareItems.TVDBID);
                int season = args.getInt(ShareItems.SEASON);
                int episode = args.getInt(ShareItems.EPISODE);
                final String shout = shouttext.getText().toString();
                final boolean isSpoiler = checkIsSpoiler.isChecked();

                if (season == -1) {
                    // shout for a show
                    new ShareUtils.TraktTask(getSherlockActivity(), getFragmentManager(), null)
                            .shout(tvdbid, shout, isSpoiler).execute();
                } else {
                    // shout for an episode
                    new ShareUtils.TraktTask(getSherlockActivity(), getFragmentManager(), null)
                            .shout(tvdbid, season, episode, shout, isSpoiler).execute();
                }
            }
        });
    }

    @Override
    public Loader<List<Shout>> onCreateLoader(int id, Bundle args) {
        return new TraktShoutsLoader(getSherlockActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<List<Shout>> loader, List<Shout> data) {
        mAdapter.setData(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Shout>> data) {
        mAdapter.setData(null);
    }

    public static class TraktShoutsLoader extends AsyncTaskLoader<List<Shout>> {

        private Bundle mArgs;

        public TraktShoutsLoader(Context context, Bundle args) {
            super(context);
            mArgs = args;
        }

        @Override
        public List<Shout> loadInBackground() {
            int tvdbId = mArgs.getInt(ShareItems.TVDBID);
            int season = mArgs.getInt(ShareItems.SEASON);
            int episode = mArgs.getInt(ShareItems.EPISODE);

            ServiceManager manager = Utils.getServiceManager(getContext());
            List<Shout> shouts = new ArrayList<Shout>();
            try {
                if (season == -1 || episode == -1) {
                    shouts = manager.showService().shouts(tvdbId).fire();
                } else {
                    shouts = manager.showService().episodeShouts(tvdbId, season, episode).fire();
                }
            } catch (TraktException te) {
                return null;
            } catch (ApiException ae) {
                return null;
            }

            return shouts;
        }
    }

    /**
     * Custom ArrayAdapter which binds {@link Shout} items to views using the
     * ViewHolder pattern and downloads avatars using the
     * {@link ImageDownloader}.
     * 
     * @author Aeon
     */
    private static class TraktShoutsAdapter extends ArrayAdapter<Shout> {
        private final ImageDownloader mImageDownloader;

        private final LayoutInflater mInflater;

        public TraktShoutsAdapter(Context context) {
            super(context, R.layout.shout);
            mImageDownloader = ImageDownloader.getInstance(context);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<Shout> data) {
            clear();
            if (data != null) {
                for (Shout item : data) {
                    add(item);
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid
            // unneccessary calls to findViewById() on each row.
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.shout, null);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.shout = (TextView) convertView.findViewById(R.id.shout);
                holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
                holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            final Shout shout = getItem(position);

            holder.name.setText(shout.user.username);
            holder.shout.setText(shout.shout);
            mImageDownloader.download(shout.user.avatar, holder.avatar, false);

            String timestamp = (String) DateUtils.getRelativeTimeSpanString(
                    shout.inserted.getTimeInMillis(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            holder.timestamp.setText(timestamp);

            return convertView;
        }

        static class ViewHolder {
            TextView name;

            TextView shout;

            TextView timestamp;

            ImageView avatar;
        }
    }
}
