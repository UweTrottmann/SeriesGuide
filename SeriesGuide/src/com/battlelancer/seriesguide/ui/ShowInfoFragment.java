
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
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
import com.actionbarsherlock.widget.ShareActionProvider;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

public class ShowInfoFragment extends SherlockFragment {

    public interface InitBundle {
        String SHOW_TVDBID = "tvdbid";
    }

    private static final String TAG = "Show Info";

    private IntentBuilder mShareIntentBuilder;

    public static ShowInfoFragment newInstance(int showTvdbId) {
        ShowInfoFragment f = new ShowInfoFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.show_info, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        onPopulateShowData();
        
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.showinfo_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.menu_share);
        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem
                .getActionProvider();
        shareActionProvider.setShareIntent(mShareIntentBuilder.getIntent());
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void onPopulateShowData() {
        final Series show = DBUtils.getShow(getActivity(), String.valueOf(getShowTvdbId()));
        if (show == null) {
            return;
        }

        TextView seriesname = (TextView) getView().findViewById(R.id.title);
        TextView overview = (TextView) getView().findViewById(R.id.TextViewShowInfoOverview);
        TextView info = (TextView) getView().findViewById(R.id.showInfo);
        TextView status = (TextView) getView().findViewById(R.id.showStatus);

        // Name
        seriesname.setText(show.getTitle());

        // Overview
        if (TextUtils.isEmpty(show.getOverview())) {
            overview.setText("");
        } else {
            overview.setText(show.getOverview());
        }

        // air time
        StringBuilder infoText = new StringBuilder();
        if (show.getAirsDayOfWeek().length() == 0 || show.getAirsTime() == -1) {
            infoText.append(getString(R.string.show_noairtime));
        } else {
            String[] values = Utils.parseMillisecondsToTime(show.getAirsTime(),
                    show.getAirsDayOfWeek(), getActivity());
            infoText.append(values[1]).append(" ").append(values[0]);
        }
        // network
        if (show.getNetwork().length() != 0) {
            infoText.append(" ").append(getString(R.string.show_network)).append(" ")
                    .append(show.getNetwork());
        }
        info.setText(infoText);

        // Running state
        if (show.getStatus() == 1) {
            TypedValue outValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.textColorSgGreen,
                    outValue, true);
            status.setTextColor(getResources().getColor(outValue.resourceId));
            status.setText(getString(R.string.show_isalive));
        } else if (show.getStatus() == 0) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // first airdate
        long airtime = Utils.buildEpisodeAirtime(show.getFirstAired(), show.getAirsTime());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoFirstAirdate),
                Utils.formatToDate(airtime, getActivity()));

        // Others
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoActors),
                Utils.splitAndKitTVDBStrings(show.getActors()));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoContentRating),
                show.getContentRating());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoGenres),
                Utils.splitAndKitTVDBStrings(show.getGenres()));
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.TextViewShowInfoRuntime),
                show.getRuntime()
                        + " " + getString(R.string.show_airtimeunit));

        // TVDb rating
        String ratingText = show.getRating();
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
        long lastEditRaw = show.getLastEdit();
        if (lastEditRaw > 0) {
            lastEdit.setText(DateUtils.formatDateTime(getActivity(), lastEditRaw * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            lastEdit.setText(R.string.unknown);
        }

        // Google Play button
        View playButton = getView().findViewById(R.id.buttonGooglePlay);
        Utils.setUpGooglePlayButton(show.getTitle(), playButton, TAG);

        // Amazon button
        View amazonButton = getView().findViewById(R.id.buttonAmazon);
        Utils.setUpAmazonButton(show.getTitle(), amazonButton, TAG);

        // IMDb button
        View imdbButton = (View) getView().findViewById(R.id.buttonShowInfoIMDB);
        final String imdbId = show.getImdbId();
        Utils.setUpImdbButton(imdbId, imdbButton, TAG, getActivity());

        // TVDb button
        View tvdbButton = (View) getView().findViewById(R.id.buttonTVDB);
        final String tvdbId = show.getId();
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
                i.putExtras(TraktShoutsActivity.createInitBundleShow(show.getTitle(),
                        getShowTvdbId()));
                startActivity(i);
            }
        });

        // Share intent
        mShareIntentBuilder = ShareCompat.IntentBuilder
                .from(getActivity())
                .setChooserTitle(R.string.share)
                .setText(
                        getString(R.string.share_checkout) + " \"" + show.getTitle()
                                + "\" " + Utils.IMDB_TITLE_URL + imdbId)
                .setType("text/plain");

        // Poster
        final ImageView poster = (ImageView) getView().findViewById(R.id.ImageViewShowInfoPoster);
        ImageProvider.getInstance(getActivity()).loadImage(poster, show.getPoster(), false);
        Utils.setPosterBackground((ImageView) getView().findViewById(R.id.background),
                show.getPoster(), getActivity());

        // trakt ratings
        TraktSummaryTask task = new TraktSummaryTask(getActivity(), getView().findViewById(
                R.id.ratingbar)).show(tvdbId);
        AndroidUtils.executeAsyncTask(task, new Void[] {
                null
        });
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
}
