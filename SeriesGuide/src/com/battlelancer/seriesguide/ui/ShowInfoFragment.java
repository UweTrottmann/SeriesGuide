
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.loaders.ShowLoader;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

import de.greenrobot.event.EventBus;

public class ShowInfoFragment extends SherlockFragment implements LoaderCallbacks<Series> {

    public interface InitBundle {
        String SHOW_TVDBID = "tvdbid";
    }

    private static final String TAG = "Show Info";
    private static final int LOADER_ID = R.layout.show_info;

    public static ShowInfoFragment newInstance(int showTvdbId) {
        ShowInfoFragment f = new ShowInfoFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    private Series mShow;
    private TraktSummaryTask mTraktTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.show_info, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_ID, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.showinfo_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_rate_trakt) {
            onRateOnTrakt();
            return true;
        } else if (itemId == R.id.menu_manage_lists) {
            ListsDialogFragment.showListsDialog(String.valueOf(getShowTvdbId()),
                    ListItemTypes.SHOW, getFragmentManager());
            return true;
        } else if (itemId == R.id.menu_share) {
            onShareShow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Series> onCreateLoader(int loaderId, Bundle args) {
        return new ShowLoader(getActivity(), getShowTvdbId());
    }

    @Override
    public void onLoadFinished(Loader<Series> loader, Series data) {
        if (data != null) {
            mShow = data;
        }
        if (isAdded()) {
            onPopulateShowData();
        }
    }

    @Override
    public void onLoaderReset(Loader<Series> loader) {
    }

    public void onEvent(TraktActionCompleteEvent event) {
        if (event.mTraktTaskArgs.getInt(TraktTask.InitBundle.TRAKTACTION) == TraktAction.RATE_EPISODE.index) {
            onLoadTraktRatings(false);
        }
    }

    private void onPopulateShowData() {
        if (mShow == null) {
            return;
        }

        TextView seriesname = (TextView) getView().findViewById(R.id.title);
        TextView overview = (TextView) getView().findViewById(R.id.TextViewShowInfoOverview);
        TextView info = (TextView) getView().findViewById(R.id.showInfo);
        TextView status = (TextView) getView().findViewById(R.id.showStatus);

        // Name
        seriesname.setText(mShow.getTitle());

        // Overview
        if (TextUtils.isEmpty(mShow.getOverview())) {
            overview.setText("");
        } else {
            overview.setText(mShow.getOverview());
        }

        // air time
        StringBuilder infoText = new StringBuilder();
        if (mShow.getAirsDayOfWeek().length() == 0 || mShow.getAirsTime() == -1) {
            infoText.append(getString(R.string.show_noairtime));
        } else {
            String[] values = Utils.parseMillisecondsToTime(mShow.getAirsTime(),
                    mShow.getAirsDayOfWeek(), getActivity());
            infoText.append(values[1]).append(" ").append(values[0]);
        }
        // network
        if (mShow.getNetwork().length() != 0) {
            infoText.append(" ").append(getString(R.string.show_network)).append(" ")
                    .append(mShow.getNetwork());
        }
        info.setText(infoText);

        // Running state
        if (mShow.getStatus() == 1) {
            TypedValue outValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.textColorSgGreen,
                    outValue, true);
            status.setTextColor(getResources().getColor(outValue.resourceId));
            status.setText(getString(R.string.show_isalive));
        } else if (mShow.getStatus() == 0) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // first airdate
        long airtime = Utils.buildEpisodeAirtime(mShow.getFirstAired(), mShow.getAirsTime());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoFirstAirdate),
                Utils.formatToDate(airtime, getActivity()));

        // Others
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoActors),
                Utils.splitAndKitTVDBStrings(mShow.getActors()));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoContentRating),
                mShow.getContentRating());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoGenres),
                Utils.splitAndKitTVDBStrings(mShow.getGenres()));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoRuntime),
                mShow.getRuntime()
                        + " " + getString(R.string.show_airtimeunit));

        // TVDb rating
        String ratingText = mShow.getRating();
        if (ratingText != null && ratingText.length() != 0) {
            RatingBar ratingBar = (RatingBar) getView().findViewById(R.id.bar);
            ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
            TextView rating = (TextView) getView().findViewById(R.id.value);
            rating.setText(ratingText + "/10");
        }
        View ratings = getView().findViewById(R.id.ratingbar);
        ratings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRateOnTrakt();
            }
        });
        ratings.setFocusable(true);
        CheatSheet.setup(ratings, R.string.menu_rate_trakt);

        // Last edit date
        TextView lastEdit = (TextView) getView().findViewById(R.id.lastEdit);
        long lastEditRaw = mShow.getLastEdit();
        if (lastEditRaw > 0) {
            lastEdit.setText(DateUtils.formatDateTime(getActivity(), lastEditRaw * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            lastEdit.setText(R.string.unknown);
        }

        // Google Play button
        View playButton = getView().findViewById(R.id.buttonGooglePlay);
        Utils.setUpGooglePlayButton(mShow.getTitle(), playButton, TAG);

        // Amazon button
        View amazonButton = getView().findViewById(R.id.buttonAmazon);
        Utils.setUpAmazonButton(mShow.getTitle(), amazonButton, TAG);

        // IMDb button
        View imdbButton = (View) getView().findViewById(R.id.buttonShowInfoIMDB);
        final String imdbId = mShow.getImdbId();
        Utils.setUpImdbButton(imdbId, imdbButton, TAG, getActivity());

        // TVDb button
        View tvdbButton = (View) getView().findViewById(R.id.buttonTVDB);
        final String tvdbId = mShow.getId();
        if (tvdbButton != null) {
            tvdbButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    fireTrackerEvent("TVDb");
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TVDB_SHOW_URL
                            + tvdbId));
                    startActivity(i);
                }
            });
        }

        // Shout button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fireTrackerEvent("Shouts");
                Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                i.putExtras(TraktShoutsActivity.createInitBundleShow(mShow.getTitle(),
                        getShowTvdbId()));
                startActivity(i);
            }
        });

        // Poster
        final ImageView poster = (ImageView) getView().findViewById(R.id.ImageViewShowInfoPoster);
        ImageProvider.getInstance(getActivity()).loadImage(poster, mShow.getPoster(), false);
//        Utils.setPosterBackground((ImageView) getView().findViewById(R.id.background),
//                mShow.getPoster(), getActivity());

        // trakt ratings
        onLoadTraktRatings(true);
    }

    private void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Context Item", label, (long) 0);
    }

    private int getShowTvdbId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onRateOnTrakt() {
        TraktRateDialogFragment newFragment = TraktRateDialogFragment
                .newInstance(getShowTvdbId());
        newFragment.show(getFragmentManager(), "traktratedialog");
    }

    private void onLoadTraktRatings(boolean isUseCachedValues) {
        if (mShow != null
                && (mTraktTask == null || mTraktTask.getStatus() != AsyncTask.Status.RUNNING)) {
            mTraktTask = new TraktSummaryTask(getActivity(), getView().findViewById(
                    R.id.ratingbar), isUseCachedValues).show(getShowTvdbId());
            AndroidUtils.executeAsyncTask(mTraktTask, new Void[] {});
        }
    }

    private void onShareShow() {
        if (mShow != null) {
            // Share intent
            IntentBuilder ib = ShareCompat.IntentBuilder
                    .from(getActivity())
                    .setChooserTitle(R.string.share)
                    .setText(
                            getString(R.string.share_checkout) + " \"" + mShow.getTitle()
                                    + "\" " + Utils.IMDB_TITLE_URL + mShow.getImdbId())
                    .setType("text/plain");
            ib.startChooser();
        }
    }
}
