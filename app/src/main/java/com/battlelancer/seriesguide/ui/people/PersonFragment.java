package com.battlelancer.seriesguide.ui.people;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ClipboardTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.battlelancer.seriesguide.util.ViewTools;
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
    @BindView(R.id.buttonPersonTmdbLink) Button buttonTmdbLink;
    @BindView(R.id.buttonPersonWebSearch) Button buttonWebSearch;

    private Person person;
    private Unbinder unbinder;

    static PersonFragment newInstance(int tmdbId) {
        PersonFragment f = new PersonFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.PERSON_TMDB_ID, tmdbId);
        f.setArguments(args);

        return f;
    }

    interface InitBundle {
        String PERSON_TMDB_ID = "person_tmdb_id";
    }

    public PersonFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_person, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        Resources.Theme theme = requireActivity().getTheme();
        ViewTools.setVectorIconLeft(theme, buttonTmdbLink, R.drawable.ic_link_black_24dp);
        ViewTools.setVectorIconLeft(theme, buttonWebSearch, R.drawable.ic_search_white_24dp);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(PeopleActivity.PERSON_LOADER_ID, null,
                personLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @OnClick(R.id.buttonPersonTmdbLink)
    public void onClickButtonTmdbLink() {
        if (person != null) {
            TmdbTools.openTmdbPerson(getActivity(), person.id, TAG);
        }
    }

    @OnLongClick(R.id.buttonPersonTmdbLink)
    public boolean onLongClickButtonTmdbLink(View view) {
        if (person == null) {
            return false;
        }
        ClipboardTools.copyTextToClipboard(view.getContext(), TmdbTools.buildPersonUrl(person.id));
        return true;
    }

    @OnClick(R.id.buttonPersonWebSearch)
    public void onClickButtonWebSearch() {
        if (person != null) {
            ServiceUtils.performWebSearch(getActivity(), person.name, TAG);
        }
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
        String biography = TextUtils.isEmpty(person.biography)
                ? getString(R.string.not_available)
                : person.biography;
        textViewBiography.setText(TextTools.textWithTmdbSource(textViewBiography.getContext(),
                biography));

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

    private LoaderManager.LoaderCallbacks<Person> personLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Person>() {
        @Override
        public Loader<Person> onCreateLoader(int id, Bundle args) {
            setProgressVisibility(true);

            int tmdbId = getArguments().getInt(InitBundle.PERSON_TMDB_ID);
            return new PersonLoader(getContext(), tmdbId);
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
