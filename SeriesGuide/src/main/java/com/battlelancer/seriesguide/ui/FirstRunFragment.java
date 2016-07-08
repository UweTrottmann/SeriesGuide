package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.CheatSheet;

/**
 * Helps the user to get familiar with the basic functions of SeriesGuide. Shown only on first start
 * up.
 */
public class FirstRunFragment extends Fragment {

    private static final String PREF_KEY_FIRSTRUN = "accepted_eula";

    protected static final String TAG = "First Run";

    private Spinner spinner;
    private OnFirstRunDismissedListener dismissedListener;

    public static FirstRunFragment newInstance() {
        return new FirstRunFragment();
    }

    public interface OnFirstRunDismissedListener {
        void onFirstRunDismissed();
    }

    public static boolean hasSeenFirstRunFragment(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KEY_FIRSTRUN, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_firstrun, container, false);

        spinner = ButterKnife.findById(view, R.id.welcome_setuplanguage);

        // add button
        view.findViewById(R.id.buttonFirstRunAddShow)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivityAddShows();
                        setFirstRunDismissed();
                        Utils.trackClick(getActivity(), TAG, "Add show");
                    }
                });

        // trakt connect button
        view.findViewById(R.id.buttonFirstRunTrakt).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ConnectTraktActivity.class));
                Utils.trackClick(getActivity(), TAG, "Connect trakt");
            }
        });

        // restore backup button
        view.findViewById(R.id.buttonFirstRunRestore)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getActivity(), DataLiberationActivity.class));
                        Utils.trackClick(getActivity(), TAG, "Restore backup");
                    }
                });

        // dismiss button
        View buttonDismiss = view.findViewById(R.id.buttonFirstRunDismiss);
        CheatSheet.setup(buttonDismiss);
        buttonDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.trackClick(getActivity(), TAG, "Dismiss");
                setFirstRunDismissed();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            dismissedListener = (OnFirstRunDismissedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFirstRunDismissedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // language chooser
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new OnLanguageSelectedListener());

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.firstrun_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_add) {
            startActivityAddShows();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startActivityAddShows() {
        startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.SEARCH_TAB_POSITION));
    }

    private void setFirstRunDismissed() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(PREF_KEY_FIRSTRUN, true).apply();

        // display shows fragment again, better use an interface!
        dismissedListener.onFirstRunDismissed();
    }

    public class OnLanguageSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            final String value = getResources().getStringArray(R.array.languageData)[pos];
            prefs.edit().putString(DisplaySettings.KEY_LANGUAGE, value).apply();
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
}
