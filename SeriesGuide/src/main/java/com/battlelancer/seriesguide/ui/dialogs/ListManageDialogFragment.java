
package com.battlelancer.seriesguide.ui.dialogs;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.util.ListsTools;

/**
 * Dialog to rename or remove a list.
 */
public class ListManageDialogFragment extends AppCompatDialogFragment {

    private static final String ARG_LIST_ID = "listId";

    public static ListManageDialogFragment newInstance(String listId) {
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
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("listmanagedialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = ListManageDialogFragment.newInstance(listId);
        newFragment.show(ft, "listmanagedialog");
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
        buttonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // remove list and items
                ListsTools.removeList(SgApp.from(getActivity()), listId);

                dismiss();
            }
        });
        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextName == null) {
                    return;
                }

                // update title
                String listName = editTextName.getText().toString().trim();
                ListsTools.renameList(SgApp.from(getActivity()), listId, listName);

                dismiss();
            }
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
