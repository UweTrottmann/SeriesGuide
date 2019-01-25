package com.battlelancer.seriesguide.traktapi;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.textfield.TextInputLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public abstract class GenericCheckInDialogFragment extends AppCompatDialogFragment {

    public interface InitBundle {

        /**
         * Title of episode or movie. <b>Required.</b>
         */
        String ITEM_TITLE = "itemtitle";

        /**
         * Movie TMDb id. <b>Required for movies.</b>
         */
        String MOVIE_TMDB_ID = "movietmdbid";

        /**
         * Season number. <b>Required for episodes.</b>
         */
        String EPISODE_TVDB_ID = "episodetvdbid";
    }

    public class CheckInDialogDismissedEvent {
    }

    @BindView(R.id.textInputLayoutCheckIn) TextInputLayout textInputLayout;
    @BindView(R.id.buttonCheckIn) View buttonCheckIn;
    @BindView(R.id.buttonCheckInPasteTitle) View buttonPasteTitle;
    @BindView(R.id.buttonCheckInClear) View buttonClear;
    @BindView(R.id.progressBarCheckIn) View progressBar;

    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use special theme with exit animation
        setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Dialog_CheckIn);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_checkin, container, false);
        unbinder = ButterKnife.bind(this, view);

        // Paste episode button
        final String itemTitle = getArguments().getString(InitBundle.ITEM_TITLE);
        final EditText editTextMessage = textInputLayout.getEditText();
        if (!TextUtils.isEmpty(itemTitle)) {
            buttonPasteTitle.setOnClickListener(v -> {
                if (editTextMessage == null) {
                    return;
                }
                int start = editTextMessage.getSelectionStart();
                int end = editTextMessage.getSelectionEnd();
                editTextMessage.getText().replace(Math.min(start, end), Math.max(start, end),
                        itemTitle, 0, itemTitle.length());
            });
        }

        // Clear button
        buttonClear.setOnClickListener(v -> {
            if (editTextMessage == null) {
                return;
            }
            editTextMessage.setText(null);
        });

        // Checkin Button
        buttonCheckIn.setOnClickListener(v -> checkIn());

        setProgressLock(false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // immediately start to check-in if the user has opted to skip entering a check-in message
        if (TraktSettings.useQuickCheckin(getContext())) {
            checkIn();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        EventBus.getDefault().post(new CheckInDialogDismissedEvent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        // done with checking in, unlock UI
        setProgressLock(false);

        if (event.wasSuccessful) {
            // all went well, dismiss ourselves
            dismissAllowingStateLoss();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(TraktTask.TraktCheckInBlockedEvent event) {
        // launch a check-in override dialog
        TraktCancelCheckinDialogFragment
                .show(getFragmentManager(), event.traktTaskArgs, event.waitMinutes);
    }

    private void checkIn() {
        // lock down UI
        setProgressLock(true);

        // connected?
        if (Utils.isNotConnected(getActivity())) {
            // no? abort
            setProgressLock(false);
            return;
        }

        // launch connect flow if trakt is not connected
        if (!TraktCredentials.ensureCredentials(getActivity())) {
            // not connected? abort
            setProgressLock(false);
            return;
        }

        // try to check in
        EditText editText = textInputLayout.getEditText();
        if (editText != null) {
            checkInTrakt(editText.getText().toString());
        }
    }

    /**
     * Start the trakt check-in task.
     */
    protected abstract void checkInTrakt(String message);

    /**
     * Disables all interactive UI elements and shows a progress indicator.
     */
    private void setProgressLock(boolean lock) {
        progressBar.setVisibility(lock ? View.VISIBLE : View.GONE);
        textInputLayout.setEnabled(!lock);
        buttonPasteTitle.setEnabled(!lock);
        buttonClear.setEnabled(!lock);
        buttonCheckIn.setEnabled(!lock);
    }
}
