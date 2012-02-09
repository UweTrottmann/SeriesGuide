
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

public class ChangesDialogFragment extends DialogFragment {
    private static final String MARKETLINK_HTTP = "http://market.android.com/details?id=com.battlelancer.seriesguide";

    private static final String MARKETLINK_APP = "market://details?id=com.battlelancer.seriesguide";

    public static final String TAG = "ChangesDialogFragment";

    public static ChangesDialogFragment show(FragmentManager fm) {
        ChangesDialogFragment f = new ChangesDialogFragment();
        FragmentTransaction ft = fm.beginTransaction();
        f.show(ft, TAG);
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final TextView message = new TextView(getActivity());
        message.setText(R.string.betawarning);
        message.setPadding(30, 30, 30, 30);
        message.setTextSize(16);

        Linkify.addLinks(message, Linkify.ALL);
        message.setMovementMethod(LinkMovementMethod.getInstance());

        return new AlertDialog.Builder(getActivity()).setTitle(R.string.app_name)
                .setIcon(android.R.drawable.ic_dialog_alert).setView(message)
                .setPositiveButton(R.string.gobreak, null)
                .setNeutralButton(getString(R.string.download_stable), new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse(MARKETLINK_APP));
                            startActivity(myIntent);
                        } catch (ActivityNotFoundException e) {
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse(MARKETLINK_HTTP));
                            startActivity(myIntent);
                        }
                    }
                }).create();
    }
}
