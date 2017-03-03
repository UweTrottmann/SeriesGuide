package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.loaders.SeasonEpisodesLoader;
import com.battlelancer.seriesguide.loaders.SeasonsLoader;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Hosts a {@link ViewPager} displaying an episode per fragment of a complete season. Used on
 * smaller screens which do not allow for multi-pane layouts or if coming from a search result
 * selection.
 */
public class EpisodeDetailsActivity extends BaseNavDrawerActivity {

    protected static final String TAG = "Episode Details";
    private static final int LOADER_EPISODE_ID = 100;
    private static final int LOADER_SEASON_ID = 101;
    private static final String STATE_EPISODE_TVDB_ID = "episodeTvdbId";

    private ImageView imageViewBackground;
    private Spinner toolbarSpinner;
    private SlidingTabLayout tabs;
    private ViewPager viewPager;

    private SeasonSpinnerAdapter spinnerAdapter;
    @Nullable private EpisodePagerAdapter episodePagerAdapter;
    private int episodeTvdbId;
    private int seasonTvdbId;
    private int showTvdbId;
    private String showTitle;
    private boolean updateShow;

    /**
     * Data which has to be passed when creating this activity. All Bundle extras are integer.
     */
    public interface InitBundle {

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int initialEpisodeTvdbId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, -1);
        if (savedInstanceState == null) {
            episodeTvdbId = initialEpisodeTvdbId;
            updateShow = true;
        } else {
            episodeTvdbId = savedInstanceState.getInt(STATE_EPISODE_TVDB_ID, initialEpisodeTvdbId);
        }

        if (episodeTvdbId == -1) {
            finish(); // nothing to display, give up.
            return;
        }

        setContentView(R.layout.activity_episode);
        setupActionBar();
        setupNavDrawer();

        setupViews();

        // start loading data
        getSupportLoaderManager().restartLoader(LOADER_EPISODE_ID, null, basicInfoLoaderCallbacks);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (episodePagerAdapter != null) {
            episodeTvdbId = episodePagerAdapter.getItemEpisodeTvdbId(viewPager.getCurrentItem());
        }
        outState.putInt(STATE_EPISODE_TVDB_ID, episodeTvdbId);
    }

    @Override
    protected void setCustomTheme() {
        // use a special immersive theme
        ThemeUtils.setImmersiveTheme(this);
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
        toolbarSpinner = ButterKnife.findById(this, R.id.sgToolbarSpinner);
        // prevent spinner from restoring selection, we do that ourselves
        toolbarSpinner.setSaveEnabled(false);

        imageViewBackground = ButterKnife.findById(this, R.id.imageViewEpisodeDetailsBackground);
        tabs = ButterKnife.findById(this, R.id.tabsEpisodeDetails);
        viewPager = ButterKnife.findById(this, R.id.pagerEpisodeDetails);

        // setup tabs
        tabs.setCustomTabView(R.layout.tabstrip_item_transparent, R.id.textViewTabStripItem);
        //noinspection ResourceType
        tabs.setSelectedIndicatorColors(ContextCompat.getColor(this,
                SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue ? R.color.white
                        : Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (seasonTvdbId == 0) {
                return true; // season tvdb not determined yet, have no idea where to go up to.
            }
            Intent upIntent = new Intent(this, EpisodesActivity.class);
            upIntent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, seasonTvdbId);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                // This activity is not part of the application's task, so
                // create a new task with a synthesized back stack.
                TaskStackBuilder
                        .create(this)
                        .addNextIntent(new Intent(this, ShowsActivity.class))
                        .addNextIntent(
                                new Intent(this, OverviewActivity.class).putExtra(
                                        OverviewActivity.EXTRA_INT_SHOW_TVDBID, showTvdbId)
                        )
                        .addNextIntent(upIntent)
                        .startActivities();
                finish();
            } else {
                /*
                 * This activity is part of the application's task, so simply
                 * navigate up to the hierarchical parent activity.
                 * NavUtils.navigateUpTo() does not seem to work here.
                 */
                upIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(upIntent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private LoaderManager.LoaderCallbacks<SeasonsLoader.Result> basicInfoLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<SeasonsLoader.Result>() {
        @Override
        public Loader<SeasonsLoader.Result> onCreateLoader(int id, Bundle args) {
            return new SeasonsLoader(EpisodeDetailsActivity.this, episodeTvdbId);
        }

        @Override
        public void onLoadFinished(Loader<SeasonsLoader.Result> loader, SeasonsLoader.Result data) {
            populateBasicInfo(data);
        }

        @Override
        public void onLoaderReset(Loader<SeasonsLoader.Result> loader) {
            // do nothing, keep existing data
        }
    };

    private void populateBasicInfo(@Nullable SeasonsLoader.Result basicInfo) {
        if (basicInfo == null) {
            // do not have minimal data, give up.
            finish();
            return;
        }

        showTvdbId = basicInfo.showTvdbId;
        showTitle = basicInfo.showTitle;

        // set show poster as background
        Utils.loadPosterBackground(this, imageViewBackground, basicInfo.showPoster);

        // set up season switcher
        spinnerAdapter = new SeasonSpinnerAdapter(this, basicInfo.seasonsOfShow);
        toolbarSpinner.setAdapter(spinnerAdapter);
        //  display the season of the given episode
        Season initialSeason = basicInfo.seasonsOfShow.get(basicInfo.seasonIndexOfEpisode);
        toolbarSpinner.setSelection(basicInfo.seasonIndexOfEpisode, false);
        loadSeason(initialSeason);

        // start listening to spinner selection changes
        toolbarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Season season = spinnerAdapter.getItem(position);
                if (season.tvdbId == seasonTvdbId) {
                    // guard against firing after layout completes
                    // still happening on custom ROMs despite workaround described at
                    // http://stackoverflow.com/a/17336944/1000543
                    return;
                }
                loadSeason(season);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // ignored
            }
        });

        if (updateShow) {
            // when shown initially, schedule a show update
            if (showTvdbId != 0) {
                updateShowDelayed(showTvdbId);
            }
        }
    }

    private void loadSeason(Season season) {
        seasonTvdbId = season.tvdbId;

        // update the activity title for accessibility
        setTitle(getString(R.string.episodes) + " " + showTitle + " "
                + SeasonTools.getSeasonString(this, season.season));

        getSupportLoaderManager().restartLoader(LOADER_SEASON_ID, null, seasonLoaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<SeasonEpisodesLoader.Result> seasonLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<SeasonEpisodesLoader.Result>() {
        @Override
        public Loader<SeasonEpisodesLoader.Result> onCreateLoader(int id, Bundle args) {
            return new SeasonEpisodesLoader(EpisodeDetailsActivity.this, seasonTvdbId,
                    episodeTvdbId);
        }

        @Override
        public void onLoadFinished(Loader<SeasonEpisodesLoader.Result> loader,
                SeasonEpisodesLoader.Result data) {
            populateSeason(data);
        }

        @Override
        public void onLoaderReset(Loader<SeasonEpisodesLoader.Result> loader) {
            // do nothing, keep existing data
        }
    };

    private void populateSeason(SeasonEpisodesLoader.Result data) {
        // setup adapter
        episodePagerAdapter = new EpisodePagerAdapter(this, getSupportFragmentManager(),
                data.episodes, false);
        viewPager.setAdapter(episodePagerAdapter);
        tabs.setViewPager(viewPager);

        viewPager.setCurrentItem(data.requestedEpisodeIndex, false);
    }

    @Override
    protected View getSnackbarParentView() {
        return findViewById(R.id.coordinatorLayoutEpisode);
    }

    public static class SeasonSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

        private final Context context;
        private final LayoutInflater inflater;
        private final List<Season> seasons;

        public SeasonSpinnerAdapter(Context context, List<Season> seasons) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.seasons = seasons;
        }

        @Override
        public int getCount() {
            return seasons.size();
        }

        @Override
        public Season getItem(int position) {
            return seasons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent,
                    R.layout.item_spinner_title);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent,
                    android.R.layout.simple_spinner_dropdown_item);
        }

        @NonNull
        private View createViewFromResource(int position, View convertView, ViewGroup parent,
                int resource) {
            TextView view;
            if (convertView == null) {
                view = (TextView) inflater.inflate(resource, parent, false);
            } else {
                view = (TextView) convertView;
            }

            Season item = getItem(position);
            view.setText(SeasonTools.getSeasonString(context, item.season));

            return view;
        }
    }

    public static class EpisodePagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Episode> mEpisodes;

        private Context mContext;

        private final boolean mIsMultiPane;

        public EpisodePagerAdapter(Context context, FragmentManager fm,
                ArrayList<Episode> episodes, boolean isMultiPane) {
            super(fm);
            mEpisodes = episodes;
            mContext = context;
            mIsMultiPane = isMultiPane;
        }

        public int getItemEpisodeTvdbId(int position) {
            return mEpisodes.get(position).episodeId;
        }

        @Override
        public Fragment getItem(int position) {
            return EpisodeDetailsFragment.newInstance(mEpisodes.get(position).episodeId,
                    mIsMultiPane);
        }

        @Override
        public int getItemPosition(Object object) {
            /*
             * This breaks the FragmentStatePagerAdapter (see
             * http://code.google.com/p/android/issues/detail?id=37990), so we
             * just destroy everything!
             */
            // EpisodeDetailsFragment fragment = (EpisodeDetailsFragment)
            // object;
            // int episodeId = fragment.getEpisodeId();
            // for (int i = 0; i < mEpisodes.size(); i++) {
            // if (episodeId == mEpisodes.get(i).episodeId) {
            // return i;
            // }
            // }
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            if (mEpisodes != null) {
                return mEpisodes.size();
            } else {
                return 0;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Episode episode = mEpisodes.get(position);
            return TextTools.getEpisodeNumber(mContext, episode.seasonNumber,
                    episode.episodeNumber);
        }

        public void updateEpisodeList(ArrayList<Episode> list) {
            if (list != null) {
                mEpisodes = list;
                notifyDataSetChanged();
            }
        }
    }
}
