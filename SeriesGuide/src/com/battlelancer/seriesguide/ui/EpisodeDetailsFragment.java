
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EpisodeDetailsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EPISODE_LOADER = 3;

    private static final String TAG = "EpisodeDetails";

    private ImageCache mImageCache;

    private FetchArtTask mArtTask;

    private SimpleCursorAdapter mAdapter;

    protected boolean isWatched;

    public static EpisodeDetailsFragment newInstance(String episodeId, boolean isShowingPoster) {
        EpisodeDetailsFragment f = new EpisodeDetailsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(Episodes._ID, episodeId);
        args.putBoolean("showposter", isShowingPoster);
        f.setArguments(args);

        return f;
    }

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(TAG, "Click", label, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
         * never use this here (on config change the view needed before removing
         * the fragment)
         */
        // if (container == null) {
        // return null;
        // }
        return inflater.inflate(R.layout.episodedetails_fragment, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/EpisodeDetails");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImageCache = ImageCache.getInstance(getActivity());

        setupAdapter();

        getLoaderManager().initLoader(EPISODE_LOADER, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mArtTask != null && mArtTask.getStatus() == AsyncTask.Status.RUNNING) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodedetails_menu, menu);

        // use an appropriate quick share button
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int lastShareAction = prefs.getInt(SeriesGuidePreferences.KEY_LAST_USED_SHARE_METHOD, -1);

        MenuItem shareAction = menu.findItem(R.id.menu_quickshare);
        if (lastShareAction > 1) {
            ShareMethod shareMethod = ShareMethod.values()[lastShareAction];
            shareAction.setTitle(shareMethod.titleRes);
            shareAction.setIcon(shareMethod.drawableRes);
        } else {
            shareAction.setEnabled(false);
            shareAction.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_quickshare: {
                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                int shareMethodIndex = prefs.getInt(
                        SeriesGuidePreferences.KEY_LAST_USED_SHARE_METHOD, -1);
                ShareMethod shareMethod = ShareMethod.values()[shareMethodIndex];

                fireTrackerEvent("Quick share (" + shareMethod.name() + ")");

                onShareEpisode(shareMethod, false);
                break;
            }
            case R.id.menu_markseen_trakt: {
                fireTrackerEvent("Mark seen (trakt)");
                onShareEpisode(ShareMethod.MARKSEEN_TRAKT, true);
                break;
            }
            case R.id.menu_rate_trakt: {
                fireTrackerEvent("Rate (trakt)");
                onShareEpisode(ShareMethod.RATE_TRAKT, true);
                break;
            }
            case R.id.menu_share_others: {
                fireTrackerEvent("Share (apps)");
                onShareEpisode(ShareMethod.OTHER_SERVICES, true);
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShareEpisode(ShareMethod shareMethod, boolean isInvalidateOptionsMenu) {
        final Cursor episode = (Cursor) getListAdapter().getItem(0);
        episode.moveToFirst();

        Bundle shareData = new Bundle();
        String episodestring = ShareUtils.onCreateShareString(getActivity(), episode);
        String sharestring = getString(R.string.share_checkout);
        sharestring += " \"" + episode.getString(EpisodeDetailsQuery.SHOW_TITLE);
        sharestring += " - " + episodestring + "\" via @SeriesGuide";
        shareData.putString(ShareItems.EPISODESTRING, episodestring);
        shareData.putString(ShareItems.SHARESTRING, sharestring);
        shareData.putString(ShareItems.IMDBID, episode.getString(EpisodeDetailsQuery.SHOW_IMDBID));
        shareData.putInt(ShareItems.EPISODE, episode.getInt(EpisodeDetailsQuery.NUMBER));
        shareData.putInt(ShareItems.SEASON, episode.getInt(EpisodeDetailsQuery.SEASON));
        shareData.putInt(ShareItems.TVDBID, episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID));

        // don't close cursor!
        // episode.close();

        ShareUtils.onShareEpisode(getActivity(), shareData, shareMethod, null);

        if (isInvalidateOptionsMenu) {
            // invalidate the options menu so a potentially new
            // quick share action is displayed
            getSherlockActivity().invalidateOptionsMenu();
        }
    }

    private void setupAdapter() {

        String[] from = new String[] {
                Episodes.TITLE, Episodes.OVERVIEW, Episodes.FIRSTAIREDMS, Episodes.DVDNUMBER,
                Episodes.DIRECTORS, Episodes.GUESTSTARS, Episodes.WRITERS, Episodes.RATING,
                Episodes.IMAGE, Shows.TITLE, Episodes.WATCHED, Shows.REF_SHOW_ID

        };
        int[] to = new int[] {
                R.id.TextViewEpisodeTitle, R.id.TextViewEpisodeDescription,
                R.id.episodedetails_root, R.id.textViewEpisodeDVDnumber,
                R.id.TextViewEpisodeDirectors, R.id.TextViewEpisodeGuestStars,
                R.id.TextViewEpisodeWriters, R.id.ratingbar, R.id.imageContainer,
                R.id.textViewEpisodeDetailsShowName, R.id.TextViewEpisodeWatchedState,
                R.id.episodedetails_root
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.episodedetails, null, from, to,
                0);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, Cursor episode, int columnIndex) {
                if (columnIndex == EpisodeDetailsQuery.WATCHED) {
                    TextView watchedState = (TextView) view;
                    isWatched = episode.getInt(EpisodeDetailsQuery.WATCHED) == 1 ? true : false;
                    watchedState.setText(isWatched ? getString(R.string.episode_iswatched)
                            : getString(R.string.episode_notwatched));
                    watchedState.setTextColor(isWatched ? Color.GREEN : Color.GRAY);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.FIRSTAIREDMS) {
                    TextView airdateText = (TextView) view
                            .findViewById(R.id.TextViewEpisodeFirstAirdate);
                    TextView airtimeText = (TextView) view.findViewById(R.id.episode_airtime);

                    // First airdate
                    final long airtime = episode.getLong(EpisodeDetailsQuery.FIRSTAIREDMS);
                    if (airtime != -1) {
                        airdateText.setText(getString(R.string.episode_firstaired) + " "
                                + Utils.formatToDate(airtime, getActivity()));
                        String[] dayAndTime = Utils.formatToTimeAndDay(airtime, getActivity());
                        airtimeText.setText(dayAndTime[2] + " (" + dayAndTime[1] + ")");
                    } else {
                        airdateText.setText(getString(R.string.episode_firstaired) + " "
                                + getString(R.string.episode_unkownairdate));
                        airtimeText.setText("");
                    }
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.DVDNUMBER) {
                    TextView dvdnumber = (TextView) view;
                    dvdnumber.setText(getString(R.string.episode_dvdnumber) + ": "
                            + episode.getString(EpisodeDetailsQuery.DVDNUMBER));
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.DIRECTORS) {
                    // Directors
                    TextView directors = (TextView) view;
                    String directorsAll = Utils.splitAndKitTVDBStrings(episode
                            .getString(EpisodeDetailsQuery.DIRECTORS));
                    directors.setText(getString(R.string.episode_directors) + " " + directorsAll);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.GUESTSTARS) {
                    // Guest stars
                    TextView gueststars = (TextView) view;
                    gueststars.setText(getString(R.string.episode_gueststars)
                            + " "
                            + Utils.splitAndKitTVDBStrings(episode
                                    .getString(EpisodeDetailsQuery.GUESTSTARS)));
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.WRITERS) {
                    // Writers
                    TextView writers = (TextView) view;
                    writers.setText(getString(R.string.episode_writers)
                            + " "
                            + Utils.splitAndKitTVDBStrings(episode
                                    .getString(EpisodeDetailsQuery.WRITERS)));
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.RATING) {
                    // TVDb rating
                    RelativeLayout rating = (RelativeLayout) view;
                    String ratingText = episode.getString(EpisodeDetailsQuery.RATING);
                    if (ratingText != null && ratingText.length() != 0) {
                        RatingBar ratingBar = (RatingBar) rating.findViewById(R.id.bar);
                        TextView ratingValue = (TextView) rating.findViewById(R.id.value);
                        ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
                        ratingValue.setText(ratingText + "/10");
                    }

                    // trakt rating
                    new TraktSummaryTask(getSherlockActivity(), rating).episode(
                            episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID),
                            episode.getInt(EpisodeDetailsQuery.SEASON),
                            episode.getInt(EpisodeDetailsQuery.NUMBER)).execute();

                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.IMAGE) {
                    FrameLayout imageContainer = (FrameLayout) view;
                    String imagePath = episode.getString(EpisodeDetailsQuery.IMAGE);
                    onLoadImage(imagePath, imageContainer);
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.SHOW_TITLE) {
                    TextView showtitle = (TextView) view;
                    showtitle.setText(episode.getString(EpisodeDetailsQuery.SHOW_TITLE));
                    final int showId = episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID);
                    showtitle.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            Intent i = new Intent(getActivity(), OverviewActivity.class);
                            i.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, showId);
                            startActivity(i);
                        }
                    });

                    // Poster
                    if (getArguments().getBoolean("showposter")
                            && Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
                        // using alpha seems not to work on eclair, so only set
                        // a background on froyo+ then
                        final ImageView background = (ImageView) getActivity().findViewById(
                                R.id.episodedetails_background);
                        Bitmap bg = mImageCache.get(episode
                                .getString(EpisodeDetailsQuery.SHOW_POSTER));
                        if (bg != null) {
                            BitmapDrawable drawable = new BitmapDrawable(getResources(), bg);
                            drawable.setAlpha(50);
                            background.setImageDrawable(drawable);
                        }
                    }
                    return true;
                }
                if (columnIndex == EpisodeDetailsQuery.REF_SHOW_ID) {
                    final int showTvdbid = episode.getInt(EpisodeDetailsQuery.REF_SHOW_ID);
                    final String seasonTvdbid = episode
                            .getString(EpisodeDetailsQuery.REF_SEASON_ID);
                    final int seasonNumber = episode.getInt(EpisodeDetailsQuery.SEASON);
                    final int episodeNumber = episode.getInt(EpisodeDetailsQuery.NUMBER);
                    final String episodeString = ShareUtils.onCreateShareString(
                            getSherlockActivity(), episode);

                    // IMDb and TVDb button
                    view.findViewById(R.id.buttonShowInfoIMDB).setVisibility(View.GONE);
                    view.findViewById(R.id.buttonTVDB).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse(Constants.TVDB_EPISODE_URL_1 + showTvdbid
                                            + Constants.TVDB_EPISODE_URL_2 + seasonTvdbid
                                            + Constants.TVDB_EPISODE_URL_3 + getEpisodeId()));
                            startActivity(i);
                        }
                    });

                    // trakt shouts button
                    final String episodeTitle = episode.getString(EpisodeDetailsQuery.TITLE);
                    view.findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // see if we are attached to a single-pane activity
                            if (getSherlockActivity() instanceof EpisodeDetailsActivity) {
                                Intent intent = new Intent(getActivity(), TraktShoutsActivity.class);
                                intent.putExtras(TraktShoutsActivity.createInitBundle(showTvdbid,
                                        seasonNumber, episodeNumber, episodeTitle));
                                startActivity(intent);
                            } else {
                                // in a multi-pane layout show the shouts in a
                                // dialog
                                TraktShoutsFragment newFragment = TraktShoutsFragment.newInstance(
                                        episodeTitle, showTvdbid, seasonNumber, episodeNumber);

                                newFragment.show(getFragmentManager(), "shouts-dialog");
                            }
                        }
                    });

                    // Check in button
                    final String showImdbid = episode.getString(EpisodeDetailsQuery.SHOW_IMDBID);
                    view.findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // display a check-in dialog
                            CheckInDialogFragment f = CheckInDialogFragment.newInstance(showImdbid,
                                    showTvdbid, seasonNumber, episodeNumber, episodeString);
                            f.show(getFragmentManager(), "checkin-dialog");
                        }
                    });

                    // Watched button
                    view.findViewById(R.id.seenButton).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fireTrackerEvent("Toggle watched");
                            onToggleWatchState();
                        }
                    });

                    // Calendar button
                    final String showTitle = episode.getString(EpisodeDetailsQuery.SHOW_TITLE);
                    final long airtime = episode.getLong(EpisodeDetailsQuery.FIRSTAIREDMS);
                    final String runtime = episode.getString(EpisodeDetailsQuery.SHOW_RUNTIME);
                    view.findViewById(R.id.calendarButton).setOnClickListener(
                            new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    fireTrackerEvent("Add to calendar");
                                    ShareUtils.onAddCalendarEvent(getSherlockActivity(), showTitle,
                                            episodeString, airtime, runtime);
                                }
                            });
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);
    }

    public String getEpisodeId() {
        return getArguments().getString(Episodes._ID);
    }

    protected void onLoadImage(String imagePath, FrameLayout container) {
        if (imagePath.length() != 0) {
            final ImageView imageView = (ImageView) container
                    .findViewById(R.id.ImageViewEpisodeImage);
            final Bitmap bitmap = mImageCache.get(imagePath);
            if (bitmap != null) {
                // image is in cache
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageBitmap(bitmap);
            } else {
                if (mArtTask == null) {
                    mArtTask = (FetchArtTask) new FetchArtTask(imagePath, imageView, getActivity())
                            .execute();
                } else if (mArtTask != null && mArtTask.getStatus() == AsyncTask.Status.FINISHED) {
                    mArtTask = (FetchArtTask) new FetchArtTask(imagePath, imageView, getActivity())
                            .execute();
                }
            }
        } else {
            // no image available
            container.setVisibility(View.GONE);
        }
    }

    private void onToggleWatchState() {
        DBUtils.markEpisode(getActivity(), getEpisodeId(), !isWatched);
        isWatched = !isWatched;
        getLoaderManager().restartLoader(EPISODE_LOADER, null, this);
    }

    interface EpisodeDetailsQuery {

        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW,
                Episodes.NUMBER, Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS,
                Episodes.DIRECTORS, Episodes.GUESTSTARS, Episodes.WRITERS,
                Tables.EPISODES + "." + Episodes.RATING, Episodes.IMAGE, Episodes.DVDNUMBER,
                Episodes.TITLE, Shows.TITLE, Shows.IMDBID, Shows.RUNTIME, Shows.POSTER,
                Seasons.REF_SEASON_ID
        };

        int _ID = 0;

        int REF_SHOW_ID = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int FIRSTAIREDMS = 6;

        int DIRECTORS = 7;

        int GUESTSTARS = 8;

        int WRITERS = 9;

        int RATING = 10;

        int IMAGE = 11;

        int DVDNUMBER = 12;

        int TITLE = 13;

        int SHOW_TITLE = 14;

        int SHOW_IMDBID = 15;

        int SHOW_RUNTIME = 16;

        int SHOW_POSTER = 17;

        int REF_SEASON_ID = 18;
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Episodes.buildEpisodeWithShowUri(getEpisodeId()),
                EpisodeDetailsQuery.PROJECTION, null, null, null);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
