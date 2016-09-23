package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import com.battlelancer.seriesguide.R;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * A dialog displaying a list of languages to choose from, applying the selected language to the
 * given show if changed.
 */
public class LanguageChoiceDialogFragment extends AppCompatDialogFragment {

    public static class LanguageChangedEvent {
        public final int showTvdbId;
        public final int selectedLanguageIndex;

        public LanguageChangedEvent(int showTvdbId, int selectedLanguageIndex) {
            this.showTvdbId = showTvdbId;
            this.selectedLanguageIndex = selectedLanguageIndex;
        }
    }

    public static final String ARG_SELECTED_LANGUAGE_POSITION = "selectedPosition";
    private static final String ARG_SHOW_TVDBID = "showTvdbId";

    public static LanguageChoiceDialogFragment newInstance(int showTvdbId,
            int selectedLanguageIndex) {
        LanguageChoiceDialogFragment f = new LanguageChoiceDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_SHOW_TVDBID, showTvdbId);
        args.putInt(ARG_SELECTED_LANGUAGE_POSITION, selectedLanguageIndex);
        f.setArguments(args);

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CharSequence[] items = getResources().getStringArray(R.array.languages);
        final int currentLanguagePosition = getArguments().getInt(ARG_SELECTED_LANGUAGE_POSITION);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.pref_language)
                .setSingleChoiceItems(
                        items,
                        currentLanguagePosition,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == currentLanguagePosition) {
                                    Timber.d("Language is unchanged, do nothing.");
                                    dismiss();
                                    return;
                                }

                                EventBus.getDefault()
                                        .post(new LanguageChangedEvent(
                                                getArguments().getInt(ARG_SHOW_TVDBID), item));
                                dismiss();
                            }
                        }).create();
    }
}
