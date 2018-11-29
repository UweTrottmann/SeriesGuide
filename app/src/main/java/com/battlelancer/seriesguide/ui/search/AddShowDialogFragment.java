package com.battlelancer.seriesguide.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Setter;
import butterknife.Unbinder;
import butterknife.ViewCollections;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.ClipboardTools;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import java.util.Date;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A {@link DialogFragment} allowing the user to decide whether to add a show to SeriesGuide.
 * Displays show details as well.
 */
public class AddShowDialogFragment extends AppCompatDialogFragment {

    public static final String TAG = "AddShowDialogFragment";
    private static final String KEY_SHOW_TVDBID = "show_tvdbid";
    private static final String KEY_SHOW_LANGUAGE = "show_language";

    /**
     * Display a {@link AddShowDialogFragment} for the given show. The language of the show should
     * be set.
     */
    public static void show(Context context, FragmentManager fm, SearchResult show) {
        // replace any currently showing add dialog (do not add it to the back stack)
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        DialogTools.safeShow(AddShowDialogFragment.newInstance(context, show), fm, ft, TAG);
    }

    /**
     * Display a {@link AddShowDialogFragment} for the given show.
     *
     * <p> Use if there is no actual search result, but just a TheTVDB id available. Uses the search
     * or fall back language.
     */
    public static void show(Context context, FragmentManager fm, int showTvdbId) {
        SearchResult fakeResult = new SearchResult();
        fakeResult.setTvdbid(showTvdbId);
        show(context, fm, fakeResult);
    }

    private static AddShowDialogFragment newInstance(Context context, SearchResult show) {
        if (TextUtils.isEmpty(show.getLanguage())) {
            // use search or fall back language
            show.setLanguage(DisplaySettings.getSearchLanguageOrFallbackIfAny(context));
        }

        AddShowDialogFragment f = new AddShowDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(InitBundle.SEARCH_RESULT, show);
        f.setArguments(args);
        return f;
    }

    public interface OnAddShowListener {

        void onAddShow(SearchResult show);
    }

    private interface InitBundle {
        String SEARCH_RESULT = "search_result";
    }

    @BindView(R.id.containerShowInfo) ViewGroup containerShowInfo;
    @BindView(R.id.textViewAddTitle) TextView title;
    @BindView(R.id.textViewAddShowMeta) TextView showmeta;
    @BindView(R.id.buttonAddLanguage) Button buttonLanguage;
    @BindView(R.id.textViewAddDescription) TextView overview;
    @BindView(R.id.textViewAddRatingValue) TextView rating;
    @BindView(R.id.textViewAddRatingRange) TextView ratingRange;
    @BindView(R.id.textViewAddGenres) TextView genres;
    @BindView(R.id.textViewAddReleased) TextView releasedTextView;
    @BindView(R.id.imageViewAddPoster) ImageView poster;

    @BindViews({
            R.id.textViewAddRatingValue,
            R.id.textViewAddRatingLabel,
            R.id.textViewAddRatingRange,
            R.id.textViewAddGenresLabel
    }) List<View> labelViews;

    @SuppressWarnings({"WeakerAccess", "ConstantConditions"})
    static final Setter<View, Boolean> VISIBLE
            = (view, value, index) -> view.setVisibility(value ? View.VISIBLE : View.INVISIBLE);

    @BindView(R.id.buttonPositive) Button buttonPositive;
    @BindView(R.id.buttonNegative) Button buttonNegative;
    @BindView(R.id.progressBarAdd) View progressBar;

    private Unbinder unbinder;
    private OnAddShowListener addShowListener;
    private SearchResult displayedShow;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            addShowListener = (OnAddShowListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnAddShowListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        displayedShow = getArguments().getParcelable(InitBundle.SEARCH_RESULT);
        if (displayedShow == null || displayedShow.getTvdbid() <= 0) {
            // invalid TVDb id
            dismiss();
            return;
        }

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dialog_addshow, container, false);
        unbinder = ButterKnife.bind(this, v);

        ViewTools.setVectorIconLeft(getActivity().getTheme(), buttonLanguage,
                R.drawable.ic_language_white_24dp);
        CheatSheet.setup(buttonLanguage, R.string.pref_language);
        ratingRange.setText(getString(R.string.format_rating_range, 10));

        // buttons
        buttonNegative.setText(R.string.dismiss);
        buttonNegative.setOnClickListener(view -> dismiss());
        buttonPositive.setVisibility(View.GONE);

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        containerShowInfo.setOnLongClickListener(view -> {
            // just copy text from views instead of re-building
            StringBuilder summaryBuilder = new StringBuilder();
            summaryBuilder.append(title.getText()).append("\n");
            summaryBuilder.append(releasedTextView.getText()).append("\n");
            summaryBuilder.append(showmeta.getText());
            return ClipboardTools.copyTextToClipboard(view.getContext(), summaryBuilder);
        });
        ClipboardTools.copyTextToClipboardOnLongClick(overview);
        ClipboardTools.copyTextToClipboardOnLongClick(genres);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        showProgressBar(true);

        // load show details
        Bundle args = new Bundle();
        args.putInt(KEY_SHOW_TVDBID, displayedShow.getTvdbid());
        args.putString(KEY_SHOW_LANGUAGE, displayedShow.getLanguage());
        LoaderManager.getInstance(this)
                .initLoader(ShowsActivity.ADD_SHOW_LOADER_ID, args, showLoaderCallbacks);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @OnClick(R.id.buttonAddLanguage)
    public void onClickButtonLanguage() {
        LanguageChoiceDialogFragment.show(getFragmentManager(),
                R.array.languageCodesShows, displayedShow.getLanguage(),
                LanguageChoiceDialogFragment.TAG_ADD_DIALOG
        );
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LanguageChoiceDialogFragment.LanguageChangedEvent event) {
        if (!LanguageChoiceDialogFragment.TAG_ADD_DIALOG.equals(event.getTag())) {
            return;
        }

        showProgressBar(true);
        overview.setVisibility(View.INVISIBLE);

        displayedShow.setLanguage(event.getSelectedLanguageCode());
        // reload show details
        Bundle args = new Bundle();
        args.putInt(KEY_SHOW_TVDBID, displayedShow.getTvdbid());
        args.putString(KEY_SHOW_LANGUAGE, displayedShow.getLanguage());
        LoaderManager.getInstance(this)
                .restartLoader(ShowsActivity.ADD_SHOW_LOADER_ID, args, showLoaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<TvdbShowLoader.Result> showLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<TvdbShowLoader.Result>() {
        @Override
        public Loader<TvdbShowLoader.Result> onCreateLoader(int id, Bundle args) {
            int showTvdbId = args.getInt(KEY_SHOW_TVDBID);
            String language = args.getString(KEY_SHOW_LANGUAGE);
            return new TvdbShowLoader(getContext(), showTvdbId, language);
        }

        @Override
        public void onLoadFinished(Loader<TvdbShowLoader.Result> loader,
                TvdbShowLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            showProgressBar(false);
            overview.setVisibility(View.VISIBLE);
            populateShowViews(data);
        }

        @Override
        public void onLoaderReset(Loader<TvdbShowLoader.Result> loader) {
            // do nothing
        }
    };

    private void populateShowViews(TvdbShowLoader.Result result) {
        Show show = result.show;
        if (show == null) {
            // failed to load, can't be added
            if (!AndroidUtils.isNetworkConnected(getActivity())) {
                overview.setText(R.string.offline);
            } else if (result.doesNotExist) {
                overview.setText(R.string.tvdb_error_does_not_exist);
            } else {
                overview.setText(getString(R.string.api_error_generic,
                        String.format("%s/%s", getString(R.string.tvdb),
                                getString(R.string.trakt))));
            }
            return;
        }
        if (result.isAdded) {
            // already added, offer to open show instead
            buttonPositive.setText(R.string.action_open);
            buttonPositive.setOnClickListener(v -> {
                startActivity(OverviewActivity.intentShow(getContext(),
                        displayedShow.getTvdbid()));
                dismiss();
            });
        } else {
            // not added, offer to add
            buttonPositive.setText(R.string.action_shows_add);
            buttonPositive.setOnClickListener(v -> {
                EventBus.getDefault()
                        .post(new AddFragment.OnAddingShowEvent(displayedShow.getTvdbid()));

                addShowListener.onAddShow(displayedShow);
                dismiss();
            });
        }
        buttonPositive.setVisibility(View.VISIBLE);

        // store title for add task
        displayedShow.setTitle(show.title);

        buttonLanguage.setText(
                LanguageTools.getShowLanguageStringFor(getContext(), displayedShow.getLanguage()));

        // title, overview
        title.setText(show.title);
        overview.setText(show.overview);

        // release year
        SpannableStringBuilder statusText = new SpannableStringBuilder();
        String releaseYear = TimeTools.getShowReleaseYear(show.first_aired);
        if (releaseYear != null) {
            statusText.append(releaseYear);
        }
        // continuing/ended status
        int encodedStatus = DataLiberationTools.encodeShowStatus(show.status);
        if (encodedStatus != ShowTools.Status.UNKNOWN) {
            String decodedStatus = ShowTools.getStatus(getActivity(), encodedStatus);
            if (decodedStatus != null) {
                if (statusText.length() > 0) {
                    statusText.append(" / "); // like "2016 / Continuing"
                }
                int currentTextLength = statusText.length();
                statusText.append(decodedStatus);
                // if continuing, paint status green
                statusText.setSpan(new TextAppearanceSpan(getActivity(),
                                encodedStatus == ShowTools.Status.CONTINUING
                                        ? R.style.TextAppearance_Body_Green
                                        : R.style.TextAppearance_Body_Secondary),
                        currentTextLength, statusText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        releasedTextView.setText(statusText);

        // next release day and time
        SpannableStringBuilder timeAndNetworkText = new SpannableStringBuilder();
        if (show.release_time != -1) {
            Date release = TimeTools.getShowReleaseDateTime(getActivity(),
                    show.release_time,
                    show.release_weekday,
                    show.release_timezone,
                    show.country,
                    show.network);
            String day = TimeTools.formatToLocalDayOrDaily(getActivity(), release,
                    show.release_weekday);
            String time = TimeTools.formatToLocalTime(getActivity(), release);
            timeAndNetworkText.append(day).append(" ").append(time);
            timeAndNetworkText.append("\n");
        }

        // network, runtime
        timeAndNetworkText.append(show.network);
        timeAndNetworkText.append("\n");
        timeAndNetworkText.append(
                getString(R.string.runtime_minutes, String.valueOf(show.runtime)));

        showmeta.setText(timeAndNetworkText);

        // rating
        rating.setText(TraktTools.buildRatingString(show.rating));

        // genres
        ViewTools.setValueOrPlaceholder(genres, TextTools.splitAndKitTVDBStrings(show.genres));

        // poster
        TvdbImageTools.loadShowPosterFitCrop(getActivity(), poster, show.poster);

        // enable adding of show, display views
        buttonPositive.setEnabled(true);
        ViewCollections.set(labelViews, VISIBLE, true);
    }

    private void showProgressBar(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
