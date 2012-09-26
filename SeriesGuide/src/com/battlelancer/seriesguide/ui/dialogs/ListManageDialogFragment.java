
package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.ui.OnListsChangedListener;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.google.analytics.tracking.android.EasyTracker;

public class ListManageDialogFragment extends DialogFragment {

    public static ListManageDialogFragment newInstance(String listId) {
        ListManageDialogFragment f = new ListManageDialogFragment();

        Bundle args = new Bundle();
        args.putString("listid", listId);
        f.setArguments(args);

        return f;
    }

    private EditText mTitle;
    private OnListsChangedListener mListener;
    private Button mButtonNegative;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        if (SeriesGuidePreferences.THEME == R.style.ICSBaseTheme) {
            setStyle(STYLE_NO_TITLE, 0);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.SeriesGuideTheme_Dialog);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.list_manage_dialog, null);

        // title
        mTitle = (EditText) layout.findViewById(R.id.title);
        mTitle.setFilters(new InputFilter[] {
                new CharAndDigitInputFilter()
        });

        // buttons
        mButtonNegative = (Button) layout.findViewById(R.id.buttonNegative);
        mButtonNegative.setText(R.string.list_remove);
        mButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // remove list and items
                String listId = getArguments().getString("listid");
                getActivity().getContentResolver().delete(Lists.buildListUri(listId), null,
                        null);
                getActivity().getContentResolver().delete(ListItems.CONTENT_URI,
                        Lists.LIST_ID + "=?", new String[] {
                            listId
                        });

                // remove tab from view pager
                mListener.onListsChanged();

                dismiss();
            }
        });
        Button buttonPositive = (Button) layout.findViewById(R.id.buttonPositive);
        buttonPositive.setText(android.R.string.ok);
        buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // prevent whitespaces/empty names
                if (mTitle.getText().toString().trim().length() == 0) {
                    return;
                }

                // update title
                String listId = getArguments().getString("listid");
                ContentValues values = new ContentValues();
                values.put(Lists.NAME, mTitle.getText().toString());
                getActivity().getContentResolver().update(Lists.buildListUri(listId), values, null,
                        null);

                // refresh view pager
                mListener.onListsChanged();

                dismiss();
            }
        });

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnListsChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnListsChangedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("List Manage Dialog");
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);

        // pre-populate list title
        String listId = getArguments().getString("listid");
        final Cursor list = getActivity().getContentResolver()
                .query(Lists.buildListUri(listId), new String[] {
                        Lists.NAME
                }, null, null, null);
        list.moveToFirst();
        mTitle.setText(list.getString(0));
        list.close();

        // do not allow removing last list, disable remove button
        Cursor lists = getActivity().getContentResolver().query(Lists.CONTENT_URI,
                new String[] {
                    Lists._ID
                }, null, null, null);
        if (lists.getCount() == 1) {
            mButtonNegative.setEnabled(false);
        }
        lists.close();
    }

    /**
     * Display a dialog which allows to edit the title of this list or remove
     * it.
     */
    public static void showListManageDialog(String listId, FragmentManager fm) {
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

    /**
     * Restricts text input to characters and digits preventing any special
     * characters.
     */
    public static class CharAndDigitInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (!(Character.isLetterOrDigit(source.charAt(i))
                || Character.isWhitespace(source.charAt(i)))) {
                    return "";
                }
            }
            return null;
        }
    }
}
