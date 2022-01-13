
package com.battlelancer.seriesguide.ui.lists;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.DialogListManageBinding;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.util.DialogTools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashSet;

/**
 * Displays a dialog to add a new list to lists.
 */
public class AddListDialogFragment extends AppCompatDialogFragment {

    private static final String TAG = "addlistdialog";

    /**
     * Display a dialog which allows to edit the title of this list or remove it.
     */
    public static void show(FragmentManager fm) {
        // replace any currently showing list dialog (do not add it to the back stack)
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        DialogTools.safeShow(new AddListDialogFragment(), fm, ft, TAG);
    }

    private DialogListManageBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DialogListManageBinding binding = DialogListManageBinding.inflate(getLayoutInflater());
        this.binding = binding;

        // title
        final EditText editTextName = binding.textInputLayoutListManageListName.getEditText();
        if (editTextName != null) {
            editTextName.addTextChangedListener(
                    new ListNameTextWatcher(requireContext(),
                            binding.textInputLayoutListManageListName, binding.buttonPositive, null));
        }

        // buttons
        binding.buttonNegative.setText(android.R.string.cancel);
        binding.buttonNegative.setOnClickListener(v -> dismiss());
        binding.buttonPositive.setText(R.string.list_add);
        binding.buttonPositive.setOnClickListener(v -> {
            if (editTextName == null) {
                return;
            }

            // add list
            String listName = editTextName.getText().toString().trim();
            ListsTools.addList(requireContext(), listName);

            dismiss();
        });
        binding.buttonPositive.setEnabled(false);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }

    /**
     * Disables the given button if the watched text has only whitespace or the list name is already
     * used. Does currently not protect against a new list resulting in the same list id (if
     * inserted just resets the properties of the existing list).
     */
    public static class ListNameTextWatcher implements TextWatcher {
        private final Context context;
        private final TextInputLayout textInputLayoutName;
        private final TextView buttonPositive;
        private final HashSet<String> listNames;
        @Nullable
        private final String currentName;

        public ListNameTextWatcher(Context context, TextInputLayout textInputLayoutName,
                TextView buttonPositive, @Nullable String currentName) {
            this.context = context;
            this.textInputLayoutName = textInputLayoutName;
            this.buttonPositive = buttonPositive;
            this.currentName = currentName;
            Cursor listNameQuery = context.getContentResolver()
                    .query(Lists.CONTENT_URI, new String[] { Lists._ID, Lists.NAME }, null, null,
                            null);
            listNames = new HashSet<>();
            if (listNameQuery != null) {
                while (listNameQuery.moveToNext()) {
                    listNames.add(listNameQuery.getString(1));
                }
                listNameQuery.close();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String name = s.toString().trim();
            if (name.length() == 0) {
                buttonPositive.setEnabled(false);
                return;
            }
            if (currentName != null && currentName.equals(name)) {
                buttonPositive.setEnabled(true);
                return;
            }
            if (listNames.contains(name)) {
                textInputLayoutName.setError(
                        context.getString(R.string.error_name_already_exists));
                textInputLayoutName.setErrorEnabled(true);
                buttonPositive.setEnabled(false);
            } else {
                textInputLayoutName.setError(null);
                textInputLayoutName.setErrorEnabled(false);
                buttonPositive.setEnabled(true);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
