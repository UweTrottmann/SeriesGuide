package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.TaskManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.greenrobot.eventbus.EventBus;

public class FirstRunView extends FrameLayout {

    private static final String PREF_KEY_FIRSTRUN = "accepted_eula";

    @IntDef({ ButtonType.DISMISS,
            ButtonType.ADD_SHOW,
            ButtonType.SIGN_IN,
            ButtonType.RESTORE_BACKUP })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ButtonType {
        int DISMISS = 0;
        int ADD_SHOW = 1;
        int SIGN_IN = 2;
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

        RelativeLayout noSpoilerView = findViewById(R.id.containerFirstRunNoSpoilers);
        final CheckBox noSpoilerCheckBox = noSpoilerView.findViewById(
                R.id.checkboxFirstRunNoSpoilers);
        Button buttonAddShow = findViewById(R.id.buttonFirstRunAddShow);
        Button buttonSignIn = findViewById(R.id.buttonFirstRunSignIn);
        Button buttonRestoreBackup = findViewById(R.id.buttonFirstRunRestore);
        ImageButton buttonDismiss = findViewById(R.id.buttonFirstRunDismiss);

        noSpoilerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // new state is inversion of current state
                boolean noSpoilers = !noSpoilerCheckBox.isChecked();
                // save
                PreferenceManager.getDefaultSharedPreferences(v.getContext()).edit()
                        .putBoolean(DisplaySettings.KEY_PREVENT_SPOILERS, noSpoilers)
                        .apply();
                // update next episode strings right away
                TaskManager.getInstance().tryNextEpisodeUpdateTask(v.getContext());
                // show
                noSpoilerCheckBox.setChecked(noSpoilers);
            }
        });
        noSpoilerCheckBox.setChecked(DisplaySettings.preventSpoilers(getContext()));
        buttonAddShow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(new ButtonEvent(FirstRunView.this, ButtonType.ADD_SHOW));
            }
        });
        buttonSignIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault()
                        .post(new ButtonEvent(FirstRunView.this, ButtonType.SIGN_IN));
            }
        });
        buttonRestoreBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault()
                        .post(new ButtonEvent(FirstRunView.this, ButtonType.RESTORE_BACKUP));
            }
        });
        buttonDismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setFirstRunDismissed();
                EventBus.getDefault().post(new ButtonEvent(FirstRunView.this, ButtonType.DISMISS));
            }
        });
    }

    private void setFirstRunDismissed() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(FirstRunView.PREF_KEY_FIRSTRUN, true).apply();
    }
}
