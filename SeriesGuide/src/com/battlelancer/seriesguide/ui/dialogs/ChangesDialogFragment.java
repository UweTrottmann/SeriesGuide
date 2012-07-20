/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.ui.dialogs;

import com.battlelancer.seriesguide.beta.R;

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
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChangesDialogFragment extends DialogFragment {
    private static final String MARKETLINK_HTTP = "http://play.google.com/store/apps/details?id=com.battlelancer.seriesguide";

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
        message.setText(R.string.betamessage);
        message.setTextSize(16);

        final TextView warning = new TextView(getActivity());
        warning.setText(R.string.betawarning);
        warning.setTextSize(16);

        Linkify.addLinks(message, Linkify.ALL);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(warning, Linkify.ALL);
        warning.setMovementMethod(LinkMovementMethod.getInstance());

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.addView(message);
        layout.addView(warning);

        ScrollView scrollView = new ScrollView(getActivity());
        scrollView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        scrollView.addView(layout);

        return new AlertDialog.Builder(getActivity()).setTitle(R.string.app_name)
                .setIcon(android.R.drawable.ic_dialog_alert).setView(scrollView)
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
