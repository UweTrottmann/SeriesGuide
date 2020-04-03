package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.util.DialogTools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        Bundle args = requireArguments();

        final CharSequence[] items = getResources().getStringArray(args.getInt("itemarray"));

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(args.getInt("dialogtitle")))
                .setSingleChoiceItems(items, args.getInt("selected"), (dialog, item) -> {
                    String value = (getResources()
                            .getStringArray(args.getInt("itemdata")))[item];
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .edit()
                            .putString(args.getString("prefkey"), value)
                            .apply();
                    dismiss();
                })
                .create();
    }
}
