package com.battlelancer.seriesguide.ui;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import com.google.android.gms.actions.SearchIntents;
import de.greenrobot.event.EventBus;

/**
 * Handles search intents and displays a {@link EpisodeSearchFragment} when needed or redirects
 * directly to an {@link EpisodeDetailsActivity}.
 */
public class SearchActivity extends BaseNavDrawerActivity implements
        AddShowDialogFragment.OnAddShowListener {

    /**
     * Which tab to select upon launch.
     */
    public static final String EXTRA_DEFAULT_TAB = "default_tab";

    public static final int ADDED_TAB_POSITION = 0;
    public static final int EPISODES_TAB_POSITION = 1;
    public static final int SEARCH_TAB_POSITION = 2;
    public static final int RECOMMENDED_TAB_POSITION = 3;
    public static final int WATCHED_TAB_POSITION = 4;
    public static final int COLLECTION_TAB_POSITION = 5;
    public static final int WATCHLIST_TAB_POSITION = 6;

    public static final int SHOWS_LOADER_ID = 100;
    public static final int EPISODES_LOADER_ID = 101;
    public static final int SEARCH_LOADER_ID = 102;
    public static final int TRAKT_BASE_LOADER_ID = 200;

    /**
     * Used by {@link ShowSearchFragment} and {@link EpisodeSearchFragment} to search as the user
     * types.
     */
    public class SearchQueryEvent {
        public final Bundle args;

        public SearchQueryEvent(Bundle args) {
            this.args = args;
        }
    }

    /**
     * Used by {@link TvdbAddFragment} to submit a query. Unlike local search it is not type and
     * search.
     */
    public class SearchQuerySubmitEvent {
        public final String query;

        public SearchQuerySubmitEvent(String query) {
            this.query = query;
        }
    }

    @BindView(R.id.containerSearchBar) View searchContainer;
    @BindView(R.id.editTextSearchBar) AutoCompleteTextView searchView;
    @BindView(R.id.imageButtonSearchClear) View clearButton;
    @BindView(R.id.tabsSearch) SlidingTabLayout tabs;
    @BindView(R.id.pagerSearch) ViewPager viewPager;
    private ProgressDialog progressDialog;

    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;
    private boolean tvdbSearchVisible;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setupActionBar();
        setupNavDrawer();

        setupViews();

        handleSearchIntent(getIntent());
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void setupViews() {
        ButterKnife.bind(this);
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText(null);
                searchView.requestFocus();
            }
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEmptyText = TextUtils.isEmpty(s);
                triggerLocalSearch(isEmptyText ? "" : s.toString());
                clearButton.setVisibility(isEmptyText ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    triggerTvdbSearch();
                    return true;
                }
                return false;
            }
        });

        // manually retrieve the auto complete view popup background to override the theme
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.autoCompleteTextViewStyle, outValue, true);
        int[] attributes = new int[] { android.R.attr.popupBackground };
        TypedArray a = getTheme().obtainStyledAttributes(outValue.data, attributes);
        if (a.hasValue(0)) {
            searchView.setDropDownBackgroundDrawable(a.getDrawable(0));
        }
        a.recycle();

        // setup search history (only used by TVDb search)
        searchHistory = new SearchHistory(this, SearchSettings.KEY_SUFFIX_THETVDB);
        searchHistoryAdapter = new ArrayAdapter<>(this,
                SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light
                        ? R.layout.item_dropdown_light : R.layout.item_dropdown,
                searchHistory.getSearchHistory());
        searchView.setThreshold(1);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                triggerTvdbSearch();
            }
        });
        // set in code as XML is overridden
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        // drop-down is auto-shown on config change, ensure it is hidden when recreating views
        searchView.dismissDropDown();

        TabStripAdapter tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
                viewPager, tabs);
        tabs.setOnPageChangeListener(pageChangeListener);

        tabsAdapter.addTab(R.string.shows, ShowSearchFragment.class, null);
        tabsAdapter.addTab(R.string.episodes, EpisodeSearchFragment.class, null);
        tabsAdapter.addTab(R.string.action_shows_add, TvdbAddFragment.class, null);
        if (TraktCredentials.get(this).hasCredentials()) {
            addTraktTab(tabsAdapter, R.string.recommended, TraktAddFragment.TYPE_RECOMMENDED);
            addTraktTab(tabsAdapter, R.string.watched_shows, TraktAddFragment.TYPE_WATCHED);
            addTraktTab(tabsAdapter, R.string.shows_collection, TraktAddFragment.TYPE_COLLECTION);
            addTraktTab(tabsAdapter, R.string.watchlist, TraktAddFragment.TYPE_WATCHLIST);
        }

        tabsAdapter.notifyTabsChanged();

        // set default tab
        if (getIntent() != null && getIntent().getExtras() != null) {
            int defaultTab = getIntent().getExtras().getInt(EXTRA_DEFAULT_TAB);
            if (defaultTab < tabsAdapter.getCount()) {
                viewPager.setCurrentItem(defaultTab);
            }
            if (defaultTab == ADDED_TAB_POSITION || defaultTab == EPISODES_TAB_POSITION) {
                showSoftKeyboardOnSearchView();
            }
        } else {
            // also show keyboard when showing first tab (added tab)
            showSoftKeyboardOnSearchView();
        }
    }

    private void showSoftKeyboardOnSearchView() {
        searchView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (searchView.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200); // have to add a little delay (http://stackoverflow.com/a/27540921/1000543)
    }

    private static void addTraktTab(TabStripAdapter tabsAdapter, @StringRes int titleResId,
            @TraktAddFragment.ListType int type) {
        Bundle args = new Bundle();
        args.putInt(TraktAddFragment.ARG_TYPE, type);
        tabsAdapter.addTab(titleResId, TraktAddFragment.class, args);
    }

    private final ViewPager.OnPageChangeListener pageChangeListener
            = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            // only display search box if it can be used
            boolean searchVisible = position <= SEARCH_TAB_POSITION;
            searchContainer.setVisibility(searchVisible ? View.VISIBLE : View.GONE);
            if (searchVisible) {
                tvdbSearchVisible = position == SEARCH_TAB_POSITION;
                searchView.setAdapter(tvdbSearchVisible ? searchHistoryAdapter : null);
                searchView.setHint(
                        tvdbSearchVisible ? R.string.checkin_searchhint : R.string.search);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSearchIntent(intent);
    }

    private void handleSearchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        // global or Google Now voice search
        if (Intent.ACTION_SEARCH.equals(action) || SearchIntents.ACTION_SEARCH.equals(action)) {
            Bundle extras = getIntent().getExtras();

            // searching episodes within a show?
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                String showTitle = appData.getString(EpisodeSearchFragment.InitBundle.SHOW_TITLE);
                if (!TextUtils.isEmpty(showTitle)) {
                    // change title + switch to episodes tab if show restriction was submitted
                    viewPager.setCurrentItem(EPISODES_TAB_POSITION);
                }
            }

            // setting the query automatically triggers a search
            String query = extras.getString(SearchManager.QUERY);
            searchView.setText(query);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data == null) {
                // no data, just stay inside search activity
                return;
            }
            String id = data.getLastPathSegment();
            displayEpisode(id);
            finish();
        }
    }

    private void triggerLocalSearch(String query) {
        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, query);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                args.putBundle(SearchManager.APP_DATA, appData);
            }
        }

        EventBus.getDefault().postSticky(new SearchQueryEvent(args));
    }

    private void triggerTvdbSearch() {
        if (tvdbSearchVisible) {
            searchView.dismissDropDown();
            // extract and post query
            String query = searchView.getText().toString().trim();
            EventBus.getDefault().post(new SearchQuerySubmitEvent(query));
            // update history
            if (query.length() > 0) {
                if (searchHistory.saveRecentSearch(query)) {
                    searchHistoryAdapter.clear();
                    searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
                }
            }
        }
    }

    private void displayEpisode(String episodeTvdbId) {
        Intent i = new Intent(this, EpisodesActivity.class);
        i.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, Integer.valueOf(episodeTvdbId));
        startActivity(i);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // check for running show removal worker
        Fragment f = getSupportFragmentManager().findFragmentByTag(RemoveShowWorkerFragment.TAG);
        if (f != null && !((RemoveShowWorkerFragment) f).isTaskFinished()) {
            showProgressDialog();
        }
        // now listen to events
        EventBus.getDefault().register(this);
    }

    @Override
    public void registerEventBus() {
        // do nothing, we handle that ourselves in onStart
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If the activity was started due to an Android Beam, handle the incoming beam
        if (getIntent() != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(
                getIntent().getAction())) {
            handleBeamIntent(getIntent());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // now prevent dialog from restoring itself (we would loose ref to it)
        hideProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // clear any previous search
        EventBus.getDefault().removeStickyEvent(SearchQueryEvent.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance(this).performAddTask(SgApp.from(this), show);
    }

    /**
     * Called from {@link com.battlelancer.seriesguide.util.RemoveShowWorkerFragment}.
     */
    @SuppressWarnings("UnusedParameters")
    public void onEventMainThread(RemoveShowWorkerFragment.OnRemovingShowEvent event) {
        showProgressDialog();
    }

    /**
     * Called from {@link com.battlelancer.seriesguide.util.RemoveShowWorkerFragment}.
     */
    @SuppressWarnings("UnusedParameters")
    public void onEventMainThread(RemoveShowWorkerFragment.OnShowRemovedEvent event) {
        hideProgressDialog();
    }

    @SuppressWarnings("UnusedParameters")
    public void onEventMainThread(TvdbAddFragment.ClearSearchHistoryEvent event) {
        if (searchHistory != null && searchHistoryAdapter != null) {
            searchHistory.clearHistory();
            searchHistoryAdapter.clear();
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    /**
     * Extracts the beamed show from the NDEF Message and displays an add dialog for the show.
     */
    private void handleBeamIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null || rawMsgs.length == 0) {
            // corrupted or invalid data
            return;
        }

        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        int showTvdbId;
        try {
            showTvdbId = Integer.valueOf(new String(msg.getRecords()[0].getPayload()));
        } catch (NumberFormatException e) {
            return;
        }

        // display add dialog
        AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
    }
}
