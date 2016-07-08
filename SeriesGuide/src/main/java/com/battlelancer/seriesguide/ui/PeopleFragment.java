package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.PeopleAdapter;
import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.widgets.EmptyView;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb2.entities.Credits;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * A fragment loading and showing a list of cast or crew members.
 */
public class PeopleFragment extends Fragment {

    /**
     * The serialization (saved instance state) Bundle key representing the activated item position.
     * Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private OnShowPersonListener mListener = sDummyListener;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private ListView mListView;
    private EmptyView mEmptyView;
    private PeopleAdapter mAdapter;
    private SmoothProgressBar mProgressBar;

    private PeopleActivity.MediaType mMediaType;
    private PeopleActivity.PeopleType mPeopleType;
    private int mTmdbId;
    private boolean mActivateOnItemClick;

    public interface OnShowPersonListener {
        void showPerson(View view, int tmdbId);
    }

    private static OnShowPersonListener sDummyListener = new OnShowPersonListener() {
        @Override
        public void showPerson(View view, int tmdbId) {
        }
    };

    public PeopleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaType = PeopleActivity.MediaType.valueOf(
                getArguments().getString(PeopleActivity.InitBundle.MEDIA_TYPE));
        mPeopleType = PeopleActivity.PeopleType.valueOf(
                getArguments().getString(PeopleActivity.InitBundle.PEOPLE_TYPE));
        mTmdbId = getArguments().getInt(PeopleActivity.InitBundle.ITEM_TMDB_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_people, container, false);

        mListView = ButterKnife.findById(rootView, R.id.listViewPeople);
        mEmptyView = ButterKnife.findById(rootView, R.id.emptyViewPeople);
        mEmptyView.setContentVisibility(View.GONE);
        mListView.setEmptyView(mEmptyView);

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        mListView.setChoiceMode(mActivateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);

        mProgressBar = ButterKnife.findById(rootView, R.id.progressBarPeople);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (OnShowPersonListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnShowPersonListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new PeopleAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PeopleListHelper.Person person = mAdapter.getItem(position);
                PeopleAdapter.ViewHolder viewHolder = (PeopleAdapter.ViewHolder) view.getTag();
                mListener.showPerson(viewHolder.headshot, person.tmdbId);
            }
        });

        mEmptyView.setButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        getLoaderManager().initLoader(PeopleActivity.PEOPLE_LOADER_ID, null,
                mCreditsLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = sDummyListener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public void refresh() {
        getLoaderManager().restartLoader(PeopleActivity.PEOPLE_LOADER_ID, null,
                mCreditsLoaderCallbacks);
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
     */
    public void setActivateOnItemClick() {
        mActivateOnItemClick = true;
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            mListView.setItemChecked(mActivatedPosition, false);
        } else {
            mListView.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    private void setProgressVisibility(boolean isVisible) {
        if (mProgressBar.getVisibility() == (isVisible ? View.VISIBLE : View.GONE)) {
            // already in desired state, avoid replaying animation
            return;
        }
        mProgressBar.startAnimation(AnimationUtils.loadAnimation(mProgressBar.getContext(),
                isVisible ? R.anim.fade_in : R.anim.fade_out));
        mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void setEmptyMessage() {
        // display error message if we are offline
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            mEmptyView.setMessage(R.string.offline);
        } else {
            mEmptyView.setMessage(R.string.people_empty);
        }
        mEmptyView.setContentVisibility(View.VISIBLE);
    }

    private LoaderManager.LoaderCallbacks<Credits> mCreditsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Credits>() {
        @Override
        public Loader<Credits> onCreateLoader(int id, Bundle args) {
            setProgressVisibility(true);

            if (mMediaType == PeopleActivity.MediaType.MOVIE) {
                return new MovieCreditsLoader(getActivity(), mTmdbId);
            } else {
                return new ShowCreditsLoader(getActivity(), mTmdbId, false);
            }
        }

        @Override
        public void onLoadFinished(Loader<Credits> loader, Credits data) {
            setProgressVisibility(false);
            setEmptyMessage();

            if (data == null) {
                mAdapter.setData(null);
                return;
            }
            if (mPeopleType == PeopleActivity.PeopleType.CAST) {
                mAdapter.setData(PeopleListHelper.transformCastToPersonList(data.cast));
            } else {
                mAdapter.setData(PeopleListHelper.transformCrewToPersonList(data.crew));
            }
        }

        @Override
        public void onLoaderReset(Loader<Credits> loader) {
            // do nothing, preferring stale data over no data
        }
    };
}
