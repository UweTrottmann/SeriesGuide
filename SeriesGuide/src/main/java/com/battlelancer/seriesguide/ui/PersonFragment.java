package com.battlelancer.seriesguide.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.loaders.PersonLoader;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb2.entities.Person;

/**
 * Displays details about a crew or cast member and their work.
 */
public class PersonFragment extends Fragment {

    private static final String TAG = "Person Details";

    @BindView(R.id.progressBarPerson) ProgressBar progressBar;

    @BindView(R.id.imageViewPersonHeadshot) ImageView imageViewHeadshot;
    @BindView(R.id.textViewPersonName) TextView textViewName;
    @BindView(R.id.textViewPersonBiography) TextView textViewBiography;

    private Person person;
    private Unbinder unbinder;

    public static PersonFragment newInstance(int tmdbId) {
        PersonFragment f = new PersonFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.PERSON_TMDB_ID, tmdbId);
        f.setArguments(args);

        return f;
    }

    public interface InitBundle {
        String PERSON_TMDB_ID = "person_tmdb_id";
    }

    public PersonFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_person, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(PeopleActivity.PERSON_LOADER_ID, null,
                mPersonLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (person != null) {
            inflater.inflate(R.menu.person_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_person_tmdb) {
            TmdbTools.openTmdbPerson(getActivity(), person.id, TAG);
            return true;
        }
        if (itemId == R.id.menu_action_person_web_search) {
            ServiceUtils.performWebSearch(getActivity(), person.name, TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populatePersonViews(Person person) {
        if (person == null) {
            // TODO display empty message
            if (!AndroidUtils.isNetworkConnected(getActivity())) {
                Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        this.person = person;

        textViewName.setText(person.name);
        textViewBiography.setText(
                TextUtils.isEmpty(person.biography) ? getString(R.string.not_available)
                        : person.biography);

        if (!TextUtils.isEmpty(person.profile_path)) {
            ServiceUtils.loadWithPicasso(getActivity(),
                    TmdbTools.buildProfileImageUrl(getActivity(), person.profile_path,
                            TmdbTools.ProfileImageSize.H632))
                    .placeholder(
                            new ColorDrawable(
                                    ContextCompat.getColor(getContext(), R.color.protection_dark)))
                    .into(imageViewHeadshot);
        }

        // show actions
        getActivity().invalidateOptionsMenu();
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

    private LoaderManager.LoaderCallbacks<Person> mPersonLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Person>() {
        @Override
        public Loader<Person> onCreateLoader(int id, Bundle args) {
            setProgressVisibility(true);

            int tmdbId = getArguments().getInt(InitBundle.PERSON_TMDB_ID);
            return new PersonLoader((SgApp) getActivity().getApplication(), tmdbId);
        }

        @Override
        public void onLoadFinished(Loader<Person> loader, Person data) {
            if (!isAdded()) {
                return;
            }
            setProgressVisibility(false);
            populatePersonViews(data);
        }

        @Override
        public void onLoaderReset(Loader<Person> loader) {
            // do nothing, preferring stale data over no data
        }
    };
}
