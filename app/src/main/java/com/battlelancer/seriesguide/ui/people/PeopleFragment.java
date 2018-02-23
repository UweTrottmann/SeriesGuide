package com.battlelancer.seriesguide.ui.people;

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
import android.widget.ProgressBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.widgets.EmptyView;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb2.entities.Credits;

/**
 * A fragment loading and showing a list of cast or crew members.
 */
public class PeopleFragment extends Fragment {

    /**
     * The serialization (saved instance state) Bundle key representing the activated item position.
     * Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private ListView listView;
    private EmptyView emptyView;
    private PeopleAdapter adapter;
    private ProgressBar progressBar;

    private OnShowPersonListener onShowPersonListener = sDummyListener;
    private PeopleActivity.MediaType mediaType;
    private PeopleActivity.PeopleType peopleType;
    private int tmdbId;
    private boolean activateOnItemClick;
    /** The current activated item position. Only used on tablets. */
    private int activatedPosition = ListView.INVALID_POSITION;

    interface OnShowPersonListener {
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

        mediaType = PeopleActivity.MediaType.valueOf(
                getArguments().getString(PeopleActivity.InitBundle.MEDIA_TYPE));
        peopleType = PeopleActivity.PeopleType.valueOf(
                getArguments().getString(PeopleActivity.InitBundle.PEOPLE_TYPE));
        tmdbId = getArguments().getInt(PeopleActivity.InitBundle.ITEM_TMDB_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_people, container, false);

        listView = rootView.findViewById(R.id.listViewPeople);
        emptyView = rootView.findViewById(R.id.emptyViewPeople);
        emptyView.setContentVisibility(View.GONE);
        listView.setEmptyView(emptyView);

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        listView.setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);

        progressBar = rootView.findViewById(R.id.progressBarPeople);

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
            onShowPersonListener = (OnShowPersonListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnShowPersonListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new PeopleAdapter(getActivity());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PeopleListHelper.Person person = adapter.getItem(position);
                PeopleAdapter.ViewHolder viewHolder = (PeopleAdapter.ViewHolder) view.getTag();
                onShowPersonListener.showPerson(viewHolder.headshot, person.tmdbId);
            }
        });

        emptyView.setButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        getLoaderManager().initLoader(PeopleActivity.PEOPLE_LOADER_ID, null,
                creditsLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        onShowPersonListener = sDummyListener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
        }
    }

    private void refresh() {
        getLoaderManager().restartLoader(PeopleActivity.PEOPLE_LOADER_ID, null,
                creditsLoaderCallbacks);
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be given the
     * 'activated' state when touched.
     */
    void setActivateOnItemClick() {
        activateOnItemClick = true;
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            listView.setItemChecked(activatedPosition, false);
        } else {
            listView.setItemChecked(position, true);
        }

        activatedPosition = position;
    }

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    private void setProgressVisibility(boolean isVisible) {
        if (progressBar.getVisibility() == (isVisible ? View.VISIBLE : View.GONE)) {
            // already in desired state, avoid replaying animation
            return;
        }
        progressBar.startAnimation(AnimationUtils.loadAnimation(progressBar.getContext(),
                isVisible ? R.anim.fade_in : R.anim.fade_out));
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void setEmptyMessage() {
        // display error message if we are offline
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            emptyView.setMessage(R.string.offline);
        } else {
            emptyView.setMessage(R.string.people_empty);
        }
        emptyView.setContentVisibility(View.VISIBLE);
    }

    private LoaderManager.LoaderCallbacks<Credits> creditsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Credits>() {
        @Override
        public Loader<Credits> onCreateLoader(int id, Bundle args) {
            setProgressVisibility(true);

            if (mediaType == PeopleActivity.MediaType.MOVIE) {
                return new MovieCreditsLoader(getContext(), tmdbId);
            } else {
                return new ShowCreditsLoader(getContext(), tmdbId, false);
            }
        }

        @Override
        public void onLoadFinished(Loader<Credits> loader, Credits data) {
            setProgressVisibility(false);
            setEmptyMessage();

            if (data == null) {
                adapter.setData(null);
                return;
            }
            if (peopleType == PeopleActivity.PeopleType.CAST) {
                adapter.setData(PeopleListHelper.transformCastToPersonList(data.cast));
            } else {
                adapter.setData(PeopleListHelper.transformCrewToPersonList(data.crew));
            }
        }

        @Override
        public void onLoaderReset(Loader<Credits> loader) {
            // do nothing, preferring stale data over no data
        }
    };
}
