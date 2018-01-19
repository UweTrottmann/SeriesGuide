package com.battlelancer.seriesguide.ui.search;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.widgets.EmptyView;
import java.util.ArrayList;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

public class TvdbAddFragment extends AddFragment {

    public class ClearSearchHistoryEvent {
    }

    public static TvdbAddFragment newInstance() {
        return new TvdbAddFragment();
    }

    private static final String KEY_QUERY = "searchQuery";
    private static final String KEY_LANGUAGE = "searchLanguage";

    private Unbinder unbinder;
    private String languageCode;
    private boolean shouldTryAnyLanguage;
    private String currentQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(KEY_QUERY);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_tvdb, container, false);
        unbinder = ButterKnife.bind(this, v);

        // set initial view states
        setProgressVisible(true, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create adapter, add trakt watchlist menu if connected to trakt
        adapter = new AddAdapter(getActivity(), new ArrayList<SearchResult>(),
                itemClickListener,
                TraktCredentials.get(getActivity()).hasCredentials(), true);

        languageCode = DisplaySettings.getSearchLanguage(getContext());

        // load data
        Bundle args = new Bundle();
        args.putString(KEY_LANGUAGE, languageCode);
        args.putString(KEY_QUERY, currentQuery);
        getLoaderManager().initLoader(SearchActivity.SEARCH_LOADER_ID, args, tvdbAddLoaderCallbacks);

        // enable menu
        setHasOptionsMenu(true);
    }

    private AddAdapter.OnItemClickListener itemClickListener
            = new AddAdapter.OnItemClickListener() {

        @Override
        public void onAddClick(SearchResult item) {
            EventBus.getDefault().post(new OnAddingShowEvent(item.getTvdbid()));
            TaskManager.getInstance().performAddTask(getContext(), item);
        }

        @Override
        public void onMenuWatchlistClick(View view, int showTvdbId) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.add_dialog_popup_menu);

            // only support adding shows to watchlist
            popupMenu.getMenu()
                    .findItem(R.id.menu_action_show_watchlist_remove)
                    .setVisible(false);

            popupMenu.setOnMenuItemClickListener(
                    new TraktAddFragment.AddItemMenuItemClickListener(getContext(), showTvdbId));
            popupMenu.show();
        }
    };

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_QUERY, currentQuery);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tvdb_add_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_search_clear_history) {
            // tell the hosting activity to clear the search view history
            EventBus.getDefault().post(new ClearSearchHistoryEvent());
            return true;
        }
        if (itemId == R.id.menu_action_shows_search_change_language) {
            displayLanguageSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    private void displayLanguageSettings() {
        // guard against onClick called after fragment is up navigated (multi-touch)
        // onSaveInstanceState might already be called
        if (isResumed()) {
            DialogFragment dialog = LanguageChoiceDialogFragment.newInstance(
                    R.array.languageCodesShowsWithAny, TextUtils.isEmpty(languageCode)
                            ? getString(R.string.language_code_any) : languageCode);
            dialog.show(getFragmentManager(), "dialog-language");
        }
    }

    private void changeLanguage(String languageCode) {
        if (getString(R.string.language_code_any).equals(languageCode)) {
            languageCode = "";
        }
        this.languageCode = languageCode;

        // save selected search language
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(DisplaySettings.KEY_LANGUAGE_SEARCH, languageCode)
                .apply();

        // prevent crash due to views not being available
        // https://fabric.io/seriesguide/android/apps/com.battlelancer.seriesguide/issues/567bff98f5d3a7f76b9e8502
        if (isVisible()) {
            // refresh results in newly selected language
            search();
        }
        Timber.d("Set search language to %s", languageCode);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LanguageChoiceDialogFragment.LanguageChangedEvent event) {
        changeLanguage(event.selectedLanguageCode);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SearchActivity.SearchQuerySubmitEvent event) {
        currentQuery = event.query;
        search();
    }

    @Override
    protected void setupEmptyView(EmptyView emptyView) {
        emptyView.setButtonText(R.string.action_try_any_language);
        emptyView.setButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shouldTryAnyLanguage && !TextUtils.isEmpty(languageCode)) {
                    // try again with any language
                    shouldTryAnyLanguage = false;
                    changeLanguage(getString(R.string.language_code_any));
                } else {
                    // already set to any language or retrying, trigger search directly
                    search();
                }
            }
        });
    }

    @Override
    protected int getTabPosition() {
        return SearchActivity.TAB_POSITION_SEARCH;
    }

    private void search() {
        Bundle args = new Bundle();
        args.putString(KEY_QUERY, currentQuery);
        args.putString(KEY_LANGUAGE, languageCode);
        getLoaderManager().restartLoader(SearchActivity.SEARCH_LOADER_ID, args,
                tvdbAddLoaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<TvdbAddLoader.Result> tvdbAddLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<TvdbAddLoader.Result>() {
        @Override
        public Loader<TvdbAddLoader.Result> onCreateLoader(int id, Bundle args) {
            setProgressVisible(true, false);

            String query = null;
            String language = null;
            if (args != null) {
                query = args.getString(KEY_QUERY);
                language = args.getString(KEY_LANGUAGE);
                if (TextUtils.isEmpty(language)) {
                    // map empty string to null to search in all languages
                    language = null;
                }
            }
            return new TvdbAddLoader(getContext(), query, language);
        }

        @Override
        public void onLoadFinished(Loader<TvdbAddLoader.Result> loader, TvdbAddLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            setSearchResults(data.results);
            setEmptyMessage(data.emptyText);
            if (data.successful && data.results.size() == 0 && !TextUtils.isEmpty(languageCode)) {
                shouldTryAnyLanguage = true;
                emptyView.setButtonText(R.string.action_try_any_language);
            } else {
                emptyView.setButtonText(R.string.action_try_again);
            }
            setProgressVisible(false, true);
        }

        @Override
        public void onLoaderReset(Loader<TvdbAddLoader.Result> loader) {
            // keep existing data
        }
    };
}
