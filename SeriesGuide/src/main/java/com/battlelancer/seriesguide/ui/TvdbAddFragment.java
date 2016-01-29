/*
 * Copyright 2014 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TvdbAddLoader;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.widgets.EmptyView;
import java.util.ArrayList;
import java.util.Collections;
import timber.log.Timber;

public class TvdbAddFragment extends AddFragment {

    public static TvdbAddFragment newInstance() {
        return new TvdbAddFragment();
    }

    private static final String KEY_QUERY = "searchQuery";
    private static final String KEY_LANGUAGE = "searchLanguage";

    @Bind(R.id.buttonAddTvdbClear) ImageButton clearButton;
    @Bind(R.id.editTextAddTvdbSearch) AutoCompleteTextView searchBox;
    @Bind(R.id.spinnerAddTvdbLanguage) Spinner spinnerLanguage;

    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;
    private String language;
    private boolean shouldTryAnyLanguage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_tvdb, container, false);
        ButterKnife.bind(this, v);

        clearButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                clearSearchTerm();
            }
        });

        searchBox.setThreshold(1);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    search();
                    return true;
                }
                return false;
            }
        });
        searchBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        searchBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                search();
            }
        });
        // set in code as XML is overridden
        searchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        // drop-down is auto-shown on config change, ensure it is hidden when recreating views
        searchBox.dismissDropDown();

        // language chooser (Supported languages + any as first option)
        CharSequence[] languageNamesArray = getResources().getTextArray(R.array.languages);
        ArrayList<CharSequence> languageNamesList = new ArrayList<>(languageNamesArray.length + 1);
        languageNamesList.add(getString(R.string.any_language));
        Collections.addAll(languageNamesList, languageNamesArray);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, languageNamesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
        final String[] languageCodes = getResources().getStringArray(R.array.languageData);
        language = DisplaySettings.getSearchLanguage(getContext());
        if (!TextUtils.isEmpty(language)) {
            for (int i = 0; i < languageCodes.length; i++) {
                if (languageCodes[i].equals(language)) {
                    spinnerLanguage.setSelection(i + 1, false);
                    break;
                }
            }
        }
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    language = "";
                } else {
                    language = languageCodes[position - 1];
                }

                // save selected search language
                PreferenceManager.getDefaultSharedPreferences(parent.getContext()).edit()
                        .putString(DisplaySettings.KEY_LANGUAGE_SEARCH, language)
                        .apply();

                // prevent crash due to views not being available
                // https://fabric.io/seriesguide/android/apps/com.battlelancer.seriesguide/issues/567bff98f5d3a7f76b9e8502
                if (isVisible()) {
                    // refresh results in newly selected language
                    search();
                }
                Timber.d("Set search language to %s", language);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // set initial view states
        setProgressVisible(true, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create adapter
        adapter = new AddAdapter(getActivity(), R.layout.item_addshow,
                new ArrayList<SearchResult>());

        // setup search history
        searchHistory = new SearchHistory(getActivity(), SearchSettings.KEY_SUFFIX_THETVDB);
        searchHistoryAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, searchHistory.getSearchHistory());
        searchBox.setAdapter(searchHistoryAdapter);

        // load data
        Bundle args = new Bundle();
        args.putString(KEY_LANGUAGE, language);
        getLoaderManager().initLoader(AddActivity.AddPagerAdapter.SEARCH_TAB_DEFAULT_POSITION, args,
                mTvdbAddCallbacks);

        // enable menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_history_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_search_clear_history) {
            if (searchHistory != null && searchHistoryAdapter != null) {
                searchHistory.clearHistory();
                searchHistoryAdapter.clear();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void setupEmptyView(EmptyView emptyView) {
        emptyView.setButtonText(R.string.action_try_any_language);
        emptyView.setButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shouldTryAnyLanguage && !TextUtils.isEmpty(language)) {
                    // not set to any language: set to any language
                    // spinner selection change triggers search
                    shouldTryAnyLanguage = false;
                    spinnerLanguage.setSelection(0);
                } else {
                    // already set to any language or retrying, trigger search directly
                    search();
                }
            }
        });
    }

    private void clearSearchTerm() {
        searchBox.setText(null);
        searchBox.requestFocus();
    }

    private void search() {
        searchBox.dismissDropDown();

        // extract query
        String query = searchBox.getText().toString().trim();

        // do search
        Bundle args = new Bundle();
        args.putString(KEY_QUERY, query);
        args.putString(KEY_LANGUAGE, language);
        getLoaderManager().restartLoader(AddActivity.AddPagerAdapter.SEARCH_TAB_DEFAULT_POSITION,
                args, mTvdbAddCallbacks);

        // update history
        if (query.length() > 0) {
            if (searchHistory.saveRecentSearch(query)) {
                searchHistoryAdapter.clear();
                searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
            }
        }
    }

    private LoaderManager.LoaderCallbacks<TvdbAddLoader.Result> mTvdbAddCallbacks
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
            return new TvdbAddLoader(getActivity(), query, language);
        }

        @Override
        public void onLoadFinished(Loader<TvdbAddLoader.Result> loader, TvdbAddLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            setSearchResults(data.results);
            setEmptyMessage(data.emptyTextResId);
            if (data.successful && data.results.size() == 0 && !TextUtils.isEmpty(language)) {
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
