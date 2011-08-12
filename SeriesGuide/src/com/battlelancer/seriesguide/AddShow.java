
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.SimpleCrypto;
import com.battlelancer.thetvdbapi.SearchResult;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.TvShow;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AddShow extends Activity {

    private static final int ADD_DIALOG = 0;

    private static final int SEARCH_NOINPUT_ERROR = 0;

    private static final int SEARCH_GENERICERROR = 1;

    private static final int SEARCH_SUCCESS = 2;

    private static final int SEARCH_SAXERROR = 3;

    private static final String TAG = "Add Show";

    private MyDataObject data;

    private ListView resultList;

    private AddShowTask addTask;

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent(TAG, "Click", label, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.add_show);

        resultList = ((ListView) findViewById(R.id.ListViewSearchResults));

        // set progress in title invisible, fix for honeycomb
        setProgressBarIndeterminateVisibility(false);

        data = (MyDataObject) getLastNonConfigurationInstance();
        if (data != null) {
            data.attach(this);

            if (data.mAddQueue != null) {
                // resume adding of leftover shows and rebuilding the fts table
                addTask = (AddShowTask) new AddShowTask(this, data.mAddQueue).execute();
            }
        } else {
            data = new MyDataObject();
            getTraktShows();
        }

        ImageButton searchButton = (ImageButton) findViewById(R.id.ButtonSearch);
        searchButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                fireTrackerEvent("Search Button");
                searchSeries();
            }
        });

        EditText searchString = (EditText) findViewById(R.id.EditTextSearchString);
        searchString.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    fireTrackerEvent("Search Keyboard");
                    searchSeries();
                    return true;
                } else {
                    return false;
                }
            }
        });

        resultList.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                data.currentPosition = arg2;
                showDialog(ADD_DIALOG);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/AddShow");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remove reference
        addTask = null;
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        data.detach(this);

        if (addTask != null && addTask.getStatus() == AsyncTask.Status.RUNNING) {
            addTask.cancel(true);
            addTask = null;
        }

        return data;
    }

    private class MyDataObject {
        private ListAdapter resultsAdapter;

        private int currentPosition;

        private SearchThread searchThread;

        private LinkedList<SearchResult> mAddQueue;

        public MyDataObject() {

        }

        public void attach(AddShow activity) {
            if (resultsAdapter != null) {
                activity.resultList.setAdapter(resultsAdapter);
            }
            if (searchThread != null) {
                searchThread.setHandler(activity.searchHandler);
                if (searchThread.isAlive()) {
                    activity.setProgressBarIndeterminateVisibility(true);
                }
            }

        }

        public void detach(AddShow activity) {
            if (addTask != null && addTask.getStatus() != AsyncTask.Status.FINISHED) {
                mAddQueue = activity.addTask.getRemainingShows();
            }
            resultsAdapter = activity.resultList.getAdapter();
            activity = null;
        }
    }

    private void searchSeries() {
        setProgressBarIndeterminateVisibility(true);
        String name = ((EditText) findViewById(R.id.EditTextSearchString)).getText().toString()
                .trim();
        if (data.searchThread != null && data.searchThread.isAlive()) {
            return;
        }
        data.searchThread = new SearchThread(searchHandler, name);
        data.searchThread.start();
    }

    private Handler searchHandler = new Handler() {
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            List<SearchResult> searchResults = (List<SearchResult>) msg.obj;
            switch (msg.arg1) {
                case SEARCH_SUCCESS:
                    if (searchResults.isEmpty()) {
                        Toast.makeText(getApplicationContext(), R.string.no_results,
                                Toast.LENGTH_LONG).show();
                    }
                    setSearchResults(searchResults);
                    break;
                case SEARCH_NOINPUT_ERROR:
                    Toast.makeText(getApplicationContext(), R.string.search_noinput,
                            Toast.LENGTH_LONG).show();
                    break;
                case SEARCH_GENERICERROR:
                    Toast.makeText(getApplicationContext(), R.string.search_error,
                            Toast.LENGTH_LONG).show();
                    break;
                case SEARCH_SAXERROR:
                    Toast.makeText(getApplicationContext(), R.string.saxerror, Toast.LENGTH_LONG)
                            .show();
                    break;
            }
            setProgressBarIndeterminateVisibility(false);
        }
    };

    private void setSearchResults(List<SearchResult> searchResults) {
        ArrayAdapter<SearchResult> adapter = new ArrayAdapter<SearchResult>(
                getApplicationContext(), R.layout.add_searchresult, R.id.TextViewAddSearchResult,
                searchResults);
        ((ListView) findViewById(R.id.ListViewSearchResults)).setAdapter(adapter);
    }

    private class SearchThread extends Thread {
        private Handler handler;

        private String name;

        public SearchThread(Handler handler, String name) {
            this.handler = handler;
            this.name = name;
        }

        public void setHandler(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            Message msg = handler.obtainMessage();
            if (name.length() == 0) {
                msg.arg1 = AddShow.SEARCH_NOINPUT_ERROR;
            } else {
                try {
                    msg.obj = TheTVDB.searchShow(name, AddShow.this);
                    msg.arg1 = AddShow.SEARCH_SUCCESS;
                } catch (IOException e) {
                    msg.arg1 = AddShow.SEARCH_GENERICERROR;
                } catch (SAXException e) {
                    msg.arg1 = AddShow.SEARCH_SAXERROR;
                }
            }
            handler.sendMessage(msg);
        }

    }

    private void addShow() {
        SearchResult show = (SearchResult) resultList.getItemAtPosition(data.currentPosition);

        clearSearchField();

        if (addTask == null || addTask.getStatus() == AsyncTask.Status.FINISHED) {
            addTask = (AddShowTask) new AddShowTask(this, show).execute();
        } else {
            // addTask is still running, try to add another show to its queue
            boolean hasAddedShow = addTask.addShow(show);
            if (!hasAddedShow) {
                addTask = (AddShowTask) new AddShowTask(this, show).execute();
            }
        }
    }

    public static class AddShowTask extends AsyncTask<Void, SearchResult, Void> {

        private static final int ADD_ALREADYEXISTS = 0;

        private static final int ADD_SUCCESS = 1;

        private static final int ADD_SAXERROR = 2;

        public static final int ADD_STARTED = 3;

        final private Context mContext;

        final private LinkedList<SearchResult> mAddQueue = new LinkedList<SearchResult>();

        private boolean isFinishedAddingShows = false;

        public AddShowTask(Context activity, SearchResult show) {
            mContext = activity;
            mAddQueue.add(show);
        }

        public AddShowTask(Context activity, LinkedList<SearchResult> shows) {
            mContext = activity;
            mAddQueue.addAll(shows);
        }

        public LinkedList<SearchResult> getRemainingShows() {
            return mAddQueue;
        }

        /**
         * Add a show to the add queue. If this returns false, the show was not
         * added because the task is finishing up. Create a new one instead.
         * 
         * @param show
         * @return
         */
        public boolean addShow(SearchResult show) {
            if (isFinishedAddingShows) {
                return false;
            } else {
                mAddQueue.add(show);
                return true;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            int result;
            boolean modifiedDB = false;

            while (!mAddQueue.isEmpty()) {
                if (isCancelled()) {
                    // only cancelled on config change, so don't rebuild fts
                    // table yet
                    return null;
                }

                SearchResult nextShow = mAddQueue.removeFirst();
                final int id = nextShow.getId();
                nextShow.setId(ADD_STARTED);
                publishProgress(nextShow);

                try {
                    if (TheTVDB.addShow(String.valueOf(id), mContext.getApplicationContext())) {
                        // success
                        result = ADD_SUCCESS;
                    } else {
                        // already exists
                        result = ADD_ALREADYEXISTS;
                    }
                    modifiedDB = true;
                } catch (SAXException e) {
                    result = ADD_SAXERROR;
                }

                nextShow.setId(result);
                publishProgress(nextShow);
            }

            isFinishedAddingShows = true;
            // renew FTS3 table
            if (modifiedDB) {
                TheTVDB.onRenewFTSTable(mContext.getApplicationContext());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(SearchResult... values) {
            String showname = values[0].getSeriesName();

            switch (values[0].getId()) {
                case ADD_STARTED: {
                    Toast.makeText(mContext.getApplicationContext(),
                            "\"" + showname + "\" " + mContext.getString(R.string.add_started),
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                case ADD_SUCCESS:
                    Toast.makeText(mContext.getApplicationContext(),
                            "\"" + showname + "\" " + mContext.getString(R.string.add_success),
                            Toast.LENGTH_SHORT).show();
                    break;
                case ADD_ALREADYEXISTS:
                    Toast.makeText(
                            mContext.getApplicationContext(),
                            "\"" + showname + "\" "
                                    + mContext.getString(R.string.add_already_exists),
                            Toast.LENGTH_LONG).show();
                    break;
                case ADD_SAXERROR:
                    Toast.makeText(
                            mContext.getApplicationContext(),
                            mContext.getString(R.string.add_error_begin) + showname
                                    + mContext.getString(R.string.add_error_end) + " ("
                                    + mContext.getString(R.string.saxerror_title) + ")",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ADD_DIALOG:
                SearchResult itemAtPosition = (SearchResult) resultList
                        .getItemAtPosition(data.currentPosition);
                AlertDialog.Builder addDialogBuilder = new AlertDialog.Builder(this);

                addDialogBuilder
                        .setTitle(itemAtPosition.getSeriesName())
                        .setMessage(itemAtPosition.getOverview())
                        .setPositiveButton(R.string.add_show,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        removeDialog(ADD_DIALOG);
                                        addShow();
                                    }
                                })
                        .setNegativeButton(R.string.dont_add_show,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        removeDialog(ADD_DIALOG);
                                    }
                                });
                AlertDialog addDialog = addDialogBuilder.create();

                return addDialog;
        }

        return null;
    }

    /**
     * Clears the search field.
     */
    private void clearSearchField() {
        ((EditText) findViewById(R.id.EditTextSearchString)).setText("");
    }

    final Handler traktResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            List<SearchResult> searchResults = (List<SearchResult>) msg.obj;
            setSearchResults(searchResults);
        }
    };

    private void getTraktShows() {
        final Context context = this;

        new Thread(new Runnable() {

            @Override
            public void run() {
                if (!ShareUtils.isTraktCredentialsValid(context)) {
                    getTrendingTraktShows();
                } else {
                    getUsersTraktShows();
                }
            }
        }).start();
    }

    private void getTrendingTraktShows() {
        ServiceManager manager = new ServiceManager();
        try {
            List<TvShow> trendingShows = manager.showService().trending().fire();
            List<SearchResult> showList = parseTvShowsToSearchResults(trendingShows);
            Message msg = traktResultHandler.obtainMessage();
            msg.obj = showList;
            traktResultHandler.sendMessage(msg);
        } catch (ApiException e) {
            return;
        }
    }

    private void getUsersTraktShows() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        ServiceManager manager = new ServiceManager();
        final String username = prefs.getString(SeriesGuidePreferences.PREF_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.PREF_TRAKTPWD, "");
        try {
            password = SimpleCrypto.decrypt(password, this);
        } catch (Exception e1) {
            // password could not be decrypted
            getTrendingTraktShows();
        }
        manager.setAuthentication(username, ShareUtils.toSHA1(password.getBytes()));
        manager.setApiKey(Constants.TRAKT_API_KEY);

        try {
            List<TvShow> usersShows = manager.userService().libraryShowsAll(username).fire();
            List<SearchResult> showList = parseTvShowsToSearchResults(usersShows);
            Message msg = traktResultHandler.obtainMessage();
            msg.obj = showList;
            traktResultHandler.sendMessage(msg);
        } catch (ApiException e) {
            return;
        }
    }

    /**
     * Parse a list of {@link TvShow} objects to a list of {@link SearchResult}
     * objects.
     * 
     * @param trendingShows
     * @return
     */
    private List<SearchResult> parseTvShowsToSearchResults(List<TvShow> trendingShows) {
        List<SearchResult> showList = new ArrayList<SearchResult>();
        Iterator<TvShow> trendingit = trendingShows.iterator();
        while (trendingit.hasNext()) {
            TvShow tvShow = (TvShow) trendingit.next();
            SearchResult show = new SearchResult();
            show.setId(Integer.valueOf(tvShow.getTvdbId()));
            show.setSeriesName(tvShow.getTitle());
            show.setOverview(tvShow.getOverview());
            showList.add(show);
        }
        return showList;
    }
}
