
package com.battlelancer.seriesguide.ui.lists;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.util.DialogTools;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Dialog to rename or remove a list.
 */
public class ListManageDialogFragment extends AppCompatDialogFragment {

    private static final String TAG = "listmanagedialog";
    private static final String ARG_LIST_ID = "listId";

    private static ListManageDialogFragment newInstance(String listId) {
        ListManageDialogFragment f = new ListManageDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_LIST_ID, listId);
        f.setArguments(args);

        return f;
    }

    /**
     * Display a dialog which allows to edit the title of this list or remove it.
     */
    public static void show(String listId, FragmentManager fm) {
        // replace any currently showing list dialog (do not add it to the back stack)
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        DialogTools.safeShow(ListManageDialogFragment.newInstance(listId), fm, ft, TAG);
    }

    @BindView(R.id.textInputLayoutListManageListName) TextInputLayout textInputLayoutName;
    private EditText editTextName;
    @BindView(R.id.buttonNegative) Button buttonNegative;
    @BindView(R.id.buttonPositive) Button buttonPositive;

    private Unbinder unbinder;
    private String listId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0);

        listId = getArguments().getString(ARG_LIST_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.dialog_list_manage, container, false);
        unbinder = ButterKnife.bind(this, layout);

        editTextName = textInputLayoutName.getEditText();

        // buttons
        buttonNegative.setEnabled(false);
        buttonNegative.setText(R.string.list_remove);
        buttonNegative.setOnClickListener(v -> {
            // remove list and items
            ListsTools.removeList(getContext(), listId);

            dismiss();
        });
        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(v -> {
            if (editTextName == null) {
                return;
            }

            // update title
            String listName = editTextName.getText().toString().trim();
            ListsTools.renameList(getContext(), listId, listName);

            dismiss();
        });

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);

        // pre-populate list title
        final Cursor list = getActivity().getContentResolver()
                .query(Lists.buildListUri(listId), new String[] {
                        Lists.NAME
                }, null, null, null);
        if (list == null) {
            // list might have been removed, or query failed
            dismiss();
            return;
        }
        if (!list.moveToFirst()) {
            // list not found
            list.close();
            dismiss();
            return;
        }
        String listName = list.getString(0);
        list.close();
        editTextName.setText(listName);
        editTextName.addTextChangedListener(
                new AddListDialogFragment.ListNameTextWatcher(getContext(), textInputLayoutName,
                        buttonPositive, listName));

        // do only allow removing if this is NOT the last list
        Cursor lists = getActivity().getContentResolver().query(Lists.CONTENT_URI,
                new String[] {
                        Lists._ID
                }, null, null, null);
        if (lists != null) {
            if (lists.getCount() > 1) {
                buttonNegative.setEnabled(true);
            }
            lists.close();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }
}
