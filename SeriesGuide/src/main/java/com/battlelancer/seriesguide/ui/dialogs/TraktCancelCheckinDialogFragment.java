/*
 * Copyright 2014 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.Response;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.InitBundle;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

/**
 * Warns about an ongoing check-in, how long it takes until it is finished. Offers to override or
 * wait out.
 */
public class TraktCancelCheckinDialogFragment extends DialogFragment {

    private int mWait;

    public static TraktCancelCheckinDialogFragment newInstance(Bundle traktTaskData, int wait) {
        TraktCancelCheckinDialogFragment f = new TraktCancelCheckinDialogFragment();
        f.setArguments(traktTaskData);
        f.mWait = wait;
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Cancel Check-In Dialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity().getApplicationContext();
        final Bundle args = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(context.getString(R.string.traktcheckin_inprogress,
                DateUtils.formatElapsedTime(mWait)));

        builder.setPositiveButton(R.string.traktcheckin_cancel, new OnClickListener() {

            @Override
            @SuppressLint("CommitTransaction")
            public void onClick(DialogInterface dialog, int which) {
                AsyncTask<String, Void, Response> cancelCheckinTask
                        = new AsyncTask<String, Void, Response>() {

                    @Override
                    protected Response doInBackground(String... params) {
                        Response r = new Response();
                        r.status = TraktStatus.FAILURE;

                        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
                        if (trakt == null) {
                            // not authenticated any longer
                            r.error = context.getString(R.string.trakt_error_credentials);
                            return r;
                        }

                        try {
                            retrofit.client.Response responseDelete = trakt.checkin()
                                    .deleteActiveCheckin();
                            if (responseDelete != null && responseDelete.getStatus() == 204) {
                                r.status = TraktStatus.SUCCESS;
                            }
                        } catch (RetrofitError e) {
                            r.error = context.getString(R.string.trakt_error_general);
                        } catch (OAuthUnauthorizedException e) {
                            TraktCredentials.get(context).setCredentialsInvalid();
                            r.error = context.getString(R.string.trakt_error_credentials);
                        }

                        return r;
                    }

                    @Override
                    protected void onPostExecute(Response r) {
                        if (TraktStatus.SUCCESS.equals(r.status)) {
                            // all good
                            Toast.makeText(context, R.string.checkin_canceled_success_trakt,
                                    Toast.LENGTH_SHORT).show();

                            // relaunch the trakt task which called us to
                            // try the check in again
                            AndroidUtils.executeOnPool(new TraktTask(context, args));
                        } else if (TraktStatus.FAILURE.equals(r.status)) {
                            // well, something went wrong
                            Toast.makeText(context, r.error, Toast.LENGTH_LONG).show();
                        }
                    }
                };

                AndroidUtils.executeOnPool(cancelCheckinTask);
            }
        });
        builder.setNegativeButton(R.string.traktcheckin_wait, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // broadcast check-in success
                EventBus.getDefault().post(new TraktTask.TraktActionCompleteEvent(
                        TraktAction.valueOf(args.getString(InitBundle.TRAKTACTION)), true, null));
            }
        });

        return builder.create();
    }
}
