package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import com.battlelancer.seriesguide.R;
import java.util.Locale;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

/**
 * A dialog displaying a list of languages to choose from, posting a {@link LanguageChangedEvent} if
 * a language different from the given one was chosen.
 */
public class LanguageChoiceDialogFragment extends AppCompatDialogFragment {

    public static class LanguageChangedEvent {
        @NonNull public final String selectedLanguageCode;

        public LanguageChangedEvent(@NonNull String selectedLanguageCode) {
            this.selectedLanguageCode = selectedLanguageCode;
        }
    }

    private static final String ARG_ARRAY_LANGUAGE_CODES = "languageCodes";
    private static final String ARG_SELECTED_LANGUAGE_CODE = "selectedLanguageCode";

    /**
     * @param selectedLanguageCode two letter ISO 639-1 language code or 'xx' meaning any language,
     * if null selects first item of languageCodes.
     */
    public static LanguageChoiceDialogFragment newInstance(@ArrayRes int languageCodes,
            @Nullable String selectedLanguageCode) {
        LanguageChoiceDialogFragment f = new LanguageChoiceDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_ARRAY_LANGUAGE_CODES, languageCodes);
        args.putString(ARG_SELECTED_LANGUAGE_CODE, selectedLanguageCode);
        f.setArguments(args);

        return f;
    }

    private String[] languageCodes;
    private int currentLanguagePosition;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int languageCodesArrayRes = getArguments().getInt(ARG_ARRAY_LANGUAGE_CODES);
        languageCodes = getResources().getStringArray(languageCodesArrayRes);

        String languageCodeAny = getString(R.string.language_code_any);
        final String[] languages = new String[languageCodes.length];
        for (int i = 0; i < languageCodes.length; i++) {
            // example: "en" for shows or "en-US" for movies
            String languageCode = languageCodes[i];
            if (languageCodeAny.equals(languageCode)) {
                languages[i] = getString(R.string.any_language);
            } else {
                languages[i] = new Locale(languageCode.substring(0, 2), "").getDisplayName();
            }
        }

        final String currentLanguageCode = getArguments().getString(ARG_SELECTED_LANGUAGE_CODE);

        currentLanguagePosition = 0;
        if (currentLanguageCode != null) {
            for (int i = 0; i < languageCodes.length; i++) {
                if (languageCodes[i].equals(currentLanguageCode)) {
                    currentLanguagePosition = i;
                    break;
                }
            }
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.pref_language)
                .setSingleChoiceItems(
                        languages,
                        currentLanguagePosition,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                postLanguageChangedEvent(item);
                            }
                        }).create();
    }

    private void postLanguageChangedEvent(int selectedLanguagePosition) {
        if (selectedLanguagePosition == currentLanguagePosition) {
            Timber.d("Language is unchanged, do nothing.");
            dismiss();
            return;
        }

        EventBus.getDefault()
                .post(new LanguageChangedEvent(languageCodes[selectedLanguagePosition]));
        dismiss();
    }
}
