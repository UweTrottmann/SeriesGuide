package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.util.DialogTools;

/**
 * A dialog displaying a list of options to choose from, saving the selected option to the given
 * preference upon selection by the user.
 */
public class SingleChoiceDialogFragment extends AppCompatDialogFragment {

    public static void show(FragmentManager fragmentManager,
            int itemArrayResource, int itemDataArrayResource, int selectedItemIndex,
            String preferenceKey, int dialogTitleResource, String tag) {
        SingleChoiceDialogFragment f = new SingleChoiceDialogFragment();

        Bundle args = new Bundle();
        args.putInt("itemarray", itemArrayResource);
        args.putInt("itemdata", itemDataArrayResource);
        args.putInt("selected", selectedItemIndex);
        args.putString("prefkey", preferenceKey);
        args.putInt("dialogtitle", dialogTitleResource);
        f.setArguments(args);

        DialogTools.safeShow(f, fragmentManager, tag);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final CharSequence[] items = getResources().getStringArray(
                getArguments().getInt("itemarray"));

        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(getArguments().getInt("dialogtitle")))
                .setSingleChoiceItems(items, getArguments().getInt("selected"),
                        (dialog, item) -> {
                            final SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(getActivity()).edit();
                            editor.putString(
                                    getArguments().getString("prefkey"),
                                    (getResources().getStringArray(getArguments().getInt(
                                            "itemdata")))[item]);
                            editor.apply();
                            dismiss();
                        }).create();
    }
}
