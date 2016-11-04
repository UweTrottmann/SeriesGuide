package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.greenrobot.eventbus.EventBus;

public class FirstRunView extends CardView {

    private static final String PREF_KEY_FIRSTRUN = "accepted_eula";

    @IntDef({ ButtonType.DISMISS,
            ButtonType.ADD_SHOW,
            ButtonType.CONNECT_TRAKT,
            ButtonType.RESTORE_BACKUP })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ButtonType {
        int DISMISS = 0;
        int ADD_SHOW = 1;
        int CONNECT_TRAKT = 2;
        int RESTORE_BACKUP = 3;
    }

    public static class ButtonEvent {
        public final FirstRunView firstRunView;
        @ButtonType public final int type;

        public ButtonEvent(FirstRunView firstRunView, @ButtonType int type) {
            this.firstRunView = firstRunView;
            this.type = type;
        }
    }

    public static boolean hasSeenFirstRunFragment(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KEY_FIRSTRUN, false);
    }

    public FirstRunView(Context context) {
        this(context, null);
    }

    public FirstRunView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.view_first_run, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Spinner languageSpinner = ButterKnife.findById(this, R.id.welcome_setuplanguage);
        Button addShowButton = ButterKnife.findById(this, R.id.buttonFirstRunAddShow);
        Button connectTraktButton = ButterKnife.findById(this, R.id.buttonFirstRunTrakt);
        Button restoreBackupButton = ButterKnife.findById(this, R.id.buttonFirstRunRestore);
        ImageButton dismissButton = ButterKnife.findById(this, R.id.buttonFirstRunDismiss);

        addShowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(new ButtonEvent(FirstRunView.this, ButtonType.ADD_SHOW));
            }
        });
        connectTraktButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault()
                        .post(new ButtonEvent(FirstRunView.this, ButtonType.CONNECT_TRAKT));
            }
        });
        restoreBackupButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault()
                        .post(new ButtonEvent(FirstRunView.this, ButtonType.RESTORE_BACKUP));
            }
        });
        dismissButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setFirstRunDismissed();
                EventBus.getDefault().post(new ButtonEvent(FirstRunView.this, ButtonType.DISMISS));
            }
        });

        // language chooser
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.languagesShows, R.layout.item_spinner_title);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        languageSpinner.setOnItemSelectedListener(new OnLanguageSelectedListener());
    }

    private void setFirstRunDismissed() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(FirstRunView.PREF_KEY_FIRSTRUN, true).apply();
    }

    public class OnLanguageSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getContext());
            final String value = getResources().getStringArray(R.array.languageCodesShows)[pos];
            prefs.edit().putString(DisplaySettings.KEY_LANGUAGE, value).apply();
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
}
