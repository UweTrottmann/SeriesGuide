
package com.battlelancer.seriesguide.ui.dialogs;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.DBUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ConfirmDeleteDialogFragment extends DialogFragment {

    /**
     * Dialog to confirm the removal of a show from the database.
     * 
     * @param showId The show to remove.
     * @return
     */
    public static ConfirmDeleteDialogFragment newInstance(String showId) {
        ConfirmDeleteDialogFragment f = new ConfirmDeleteDialogFragment();

        Bundle args = new Bundle();
        args.putString("showid", showId);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String showId = getArguments().getString("showid");

        final Cursor show = getActivity().getContentResolver().query(Shows.buildShowUri(showId),
                new String[] {
                    Shows.TITLE
                }, null, null, null);

        String showName = getString(R.string.unknown);
        if (show != null && show.moveToFirst()) {
            showName = show.getString(0);
        }

        show.close();

        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.confirm_delete, showName))
                .setPositiveButton(getString(R.string.delete_show), new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        final ProgressDialog progress = new ProgressDialog(getActivity());
                        progress.setCancelable(false);
                        progress.show();

                        new Thread(new Runnable() {
                            public void run() {
                                DBUtils.deleteShow(getActivity(), getArguments()
                                        .getString("showid"));
                                if (progress.isShowing()) {
                                    progress.dismiss();
                                }
                            }
                        }).start();
                    }
                }).setNegativeButton(getString(R.string.dontdelete_show), null).create();
    }
}
