
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.thetvdbapi.SearchResult;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.SupportActivity;

/**
 * A DialogFragment allowing the user to decide whether to add a show to his show
 * database.
 * 
 * @author Uwe Trottmann
 */
public class AddDialogFragment extends DialogFragment {

    public interface OnAddShowListener {
        public void onAddShow(SearchResult show);
    }

    private SearchResult mShow;

    private OnAddShowListener mListener;

    private AddDialogFragment(SearchResult show) {
        mShow = show;
    }

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAddShowListener) activity.asActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAddShowListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity()).setTitle(mShow.title)
                .setMessage(mShow.overview)
                .setPositiveButton(R.string.add_show, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mListener.onAddShow(mShow);
                    }
                }).setNegativeButton(R.string.dont_add_show, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
    }

    /**
     * Display a dialog which asks if the user wants to add the given show to
     * his show database. If necessary an AsyncTask will be started which takes
     * care of adding the show.
     * 
     * @param show
     * @param fm
     */
    public static void showAddDialog(SearchResult show, FragmentManager fm) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
    
        // Create and show the dialog.
        DialogFragment newFragment = new AddDialogFragment(show);
        newFragment.show(ft, "dialog");
    }
}
