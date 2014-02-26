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

import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.InitBundle;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.R;

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
        final boolean isEpisodeNotMovie =
                TraktAction.valueOf(args.getString(InitBundle.TRAKTACTION))
                        == TraktAction.CHECKIN_EPISODE;

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

                        Trakt manager = ServiceUtils.getTraktWithAuth(context);
                        if (manager == null) {
                            // not authenticated any longer
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = context.getString(R.string.trakt_error_credentials);
                            return r;
                        }

                        Response response;
                        try {
                            if (isEpisodeNotMovie) {
                                response = manager.showService().cancelcheckin();
                            } else {
                                response = manager.movieService().cancelcheckin();
                            }
                        } catch (RetrofitError e) {
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = e.getMessage();
                            return r;
                        }
                        return response;
                    }

                    @Override
                    protected void onPostExecute(Response r) {
                        if (TraktStatus.SUCCESS.equals(r.status)) {
                            // all good
                            Toast.makeText(context, R.string.checkin_canceled_success_trakt,
                                    Toast.LENGTH_SHORT).show();

                            // relaunch the trakt task which called us to
                            // try the check in again
                            AndroidUtils.executeAsyncTask(new TraktTask(context, args));
                        } else if (TraktStatus.FAILURE.equals(r.status)) {
                            // well, something went wrong
                            Toast.makeText(context,
                                    context.getString(R.string.trakt_error) + ": " + r.error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                };

                cancelCheckinTask.execute();
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
