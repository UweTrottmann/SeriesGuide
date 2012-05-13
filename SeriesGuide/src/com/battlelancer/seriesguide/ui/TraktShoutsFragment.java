
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Shout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A hybrid of {@link DialogFragment} and {@link ListFragment} to display show
 * or episode shouts and for posting own shouts.
 */
public class TraktShoutsFragment extends SherlockDialogFragment implements
        LoaderCallbacks<List<Shout>>, OnTraktActionCompleteListener {

    /**
     * Build a {@link TraktShoutsFragment} for shouts of an episode.
     * 
     * @param title
     * @param tvdbId
     * @param season
     * @param episode
     * @return
     */
    public static TraktShoutsFragment newInstance(String title, int tvdbId, int season, int episode) {
        TraktShoutsFragment f = new TraktShoutsFragment();
        Bundle args = new Bundle();
        args.putString(ShareItems.SHARESTRING, title);
        args.putInt(ShareItems.TVDBID, tvdbId);
        args.putInt(ShareItems.SEASON, season);
        args.putInt(ShareItems.EPISODE, episode);
        f.setArguments(args);
        return f;
    }

    /**
     * Build a {@link TraktShoutsFragment} for shouts of a show.
     * 
     * @param title
     * @param tvdbId
     * @return
     */
    public static TraktShoutsFragment newInstance(String title, int tvdbId) {
        TraktShoutsFragment f = new TraktShoutsFragment();
        Bundle args = new Bundle();
        args.putString(ShareItems.SHARESTRING, title);
        args.putInt(ShareItems.TVDBID, tvdbId);
        args.putInt(ShareItems.EPISODE, 0);
        f.setArguments(args);
        return f;
    }

    static final int INTERNAL_EMPTY_ID = 0x00ff0001;

    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    final private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    TraktShoutsAdapter mAdapter;

    ListView mList;

    View mEmptyView;

    TextView mStandardEmptyView;

    View mProgressContainer;

    View mListContainer;

    CharSequence mEmptyText;

    boolean mListShown;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.shouts_fragment, container, false);
        final EditText shouttext = (EditText) v.findViewById(R.id.shouttext);
        final CheckBox checkIsSpoiler = (CheckBox) v.findViewById(R.id.checkIsSpoiler);

        v.findViewById(R.id.shoutbutton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // prevent empty shouts
                final String shout = shouttext.getText().toString();
                if (shout.length() == 0) {
                    return;
                }

                // disable the shout button
                v.setEnabled(false);

                final Bundle args = getArguments();
                int tvdbid = args.getInt(ShareItems.TVDBID);
                int episode = args.getInt(ShareItems.EPISODE);
                final boolean isSpoiler = checkIsSpoiler.isChecked();

                if (episode == 0) {
                    // shout for a show
                    new TraktTask(getSherlockActivity(), getFragmentManager(),
                            TraktShoutsFragment.this).shout(tvdbid, shout, isSpoiler).execute();
                } else {
                    // shout for an episode
                    int season = args.getInt(ShareItems.SEASON);
                    new TraktTask(getSherlockActivity(), getFragmentManager(),
                            TraktShoutsFragment.this).shout(tvdbid, season, episode, shout,
                            isSpoiler).execute();
                }
            }
        });

        if (getDialog() != null) {
            getDialog()
                    .setTitle(
                            getString(R.string.shouts_for,
                                    getArguments().getString(ShareItems.SHARESTRING)));
        }
        return v;
    }

    /**
     * Attach to list view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Shouts");

        mAdapter = new TraktShoutsAdapter(getActivity());
        setListAdapter(mAdapter);

        // nag about no connectivity
        if (!Utils.isNetworkConnected(getSherlockActivity())) {
            setListShown(true);
            ((TextView) mEmptyView).setText(R.string.offline);
        } else {
            setListShown(false);
            getLoaderManager().initLoader(0, getArguments(), this);
            mHandler.postDelayed(mUpdateShoutsRunnable, DateUtils.MINUTE_IN_MILLIS);
        }
    }

    private Runnable mUpdateShoutsRunnable = new Runnable() {
        @Override
        public void run() {
            getLoaderManager().restartLoader(0, getArguments(), TraktShoutsFragment.this);
            mHandler.postDelayed(mUpdateShoutsRunnable, DateUtils.MINUTE_IN_MILLIS);
        }
    };

    /**
     * Detach from list view.
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mHandler.removeCallbacks(mUpdateShoutsRunnable);
        mList = null;
        mListShown = false;
        mEmptyView = mProgressContainer = mListContainer = null;
        mStandardEmptyView = null;
        super.onDestroyView();
    }

    /**
     * Provide the cursor for the list view.
     */
    public void setListAdapter(TraktShoutsAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            if (!mListShown && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter. It is now time to show it.
                setListShown(true, getView().getWindowToken() != null);
            }
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        final Shout shout = (Shout) l.getItemAtPosition(position);

        if (shout.spoiler) {
            // if shout is a spoiler, first click will reveal the shout
            shout.spoiler = false;
            TextView shoutText = (TextView) v.findViewById(R.id.shout);
            if (shoutText != null) {
                shoutText.setText(shout.shout);
            }
        } else {
            // open trakt user profile web page
            if (shout.user.url != null) {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(shout.user.url));
                startActivity(myIntent);
            }
        }
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

    /**
     * Control whether the list is being displayed. You can make it not
     * displayed if you are waiting for the initial data to show in it. During
     * this time an indeterminant progress indicator will be shown instead.
     * <p>
     * Applications do not normally need to use this themselves. The default
     * behavior of ListFragment is to start with the list not being shown, only
     * showing it once an adapter is given with
     * {@link #setListAdapter(ListAdapter)}. If the list at that point had not
     * been shown, when it does get shown it will be do without the user ever
     * seeing the hidden state.
     * 
     * @param shown If true, the list view is shown; if false, the progress
     *            indicator. The initial value is true.
     */
    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }

    /**
     * Like {@link #setListShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    /**
     * Control whether the list is being displayed. You can make it not
     * displayed if you are waiting for the initial data to show in it. During
     * this time an indeterminant progress indicator will be shown instead.
     * 
     * @param shown If true, the list view is shown; if false, the progress
     *            indicator. The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     *            new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mListShown == shown) {
            return;
        }
        mListShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                        android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof ListView) {
            mList = (ListView) root;
        } else {
            mStandardEmptyView = (TextView) root.findViewById(INTERNAL_EMPTY_ID);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            } else {
                mStandardEmptyView.setVisibility(View.GONE);
            }
            mProgressContainer = root.findViewById(R.id.progress_container);
            mListContainer = root.findViewById(R.id.list_container);
            View rawListView = root.findViewById(android.R.id.list);
            if (!(rawListView instanceof ListView)) {
                if (rawListView == null) {
                    throw new RuntimeException(
                            "Your content must have a ListView whose id attribute is "
                                    + "'android.R.id.list'");
                }
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' "
                                + "that is not a ListView class");
            }
            mList = (ListView) rawListView;
            if (mEmptyView != null) {
                mList.setEmptyView(mEmptyView);
            } else if (mEmptyText != null) {
                mStandardEmptyView.setText(mEmptyText);
                mList.setEmptyView(mStandardEmptyView);
            }
        }
        mListShown = true;
        mList.setOnItemClickListener(mOnClickListener);
        if (mAdapter != null) {
            TraktShoutsAdapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }

    public static class TraktShoutsLoader extends AsyncTaskLoader<List<Shout>> {

        private Bundle mArgs;

        private List<Shout> mResults;

        public TraktShoutsLoader(Context context, Bundle args) {
            super(context);
            mArgs = args;
        }

        @Override
        public List<Shout> loadInBackground() {
            int tvdbId = mArgs.getInt(ShareItems.TVDBID);
            int episode = mArgs.getInt(ShareItems.EPISODE);

            ServiceManager manager = Utils.getServiceManager(getContext());
            List<Shout> shouts = new ArrayList<Shout>();
            try {
                if (episode == 0) {
                    shouts = manager.showService().shouts(tvdbId).fire();
                } else {
                    int season = mArgs.getInt(ShareItems.SEASON);
                    shouts = manager.showService().episodeShouts(tvdbId, season, episode).fire();
                }
            } catch (TraktException te) {
                return null;
            } catch (ApiException ae) {
                return null;
            }

            return shouts;
        }

        /**
         * Called when there is new data to deliver to the client. The super
         * class will take care of delivering it; the implementation here just
         * adds a little more logic.
         */
        @Override
        public void deliverResult(List<Shout> results) {
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (results != null) {
                    onReleaseResources(results);
                }
            }
            List<Shout> oldResults = results;
            mResults = results;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(results);
            }

            if (oldResults != null) {
                onReleaseResources(oldResults);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mResults != null) {
                deliverResult(mResults);
            } else {
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override
        public void onCanceled(List<Shout> data) {
            super.onCanceled(data);

            onReleaseResources(data);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release resources
            if (mResults != null) {
                onReleaseResources(mResults);
                mResults = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated with
         * an actively loaded data set.
         */
        protected void onReleaseResources(List<Shout> apps) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }
    }

    /**
     * Custom ArrayAdapter which binds {@link Shout} items to views using the
     * ViewHolder pattern and downloads avatars using the
     * {@link ImageDownloader}.
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
            mImageDownloader.download(shout.user.avatar, holder.avatar, false);

            if (shout.spoiler) {
                holder.shout.setText(R.string.isspoiler);
            } else {
                holder.shout.setText(shout.shout);
            }

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

    @Override
    public void onTraktActionComplete(boolean wasSuccessfull) {
        if (getView() != null) {

            EditText shoutText = (EditText) getView().findViewById(R.id.shouttext);
            View button = getView().findViewById(R.id.shoutbutton);

            if (shoutText != null && button != null) {
                // reenable the shout button
                button.setEnabled(true);

                if (wasSuccessfull) {
                    // clear the text field and show recent shout
                    shoutText.setText("");
                    getLoaderManager().restartLoader(0, getArguments(), this);
                }
            }
        }
    }
}
