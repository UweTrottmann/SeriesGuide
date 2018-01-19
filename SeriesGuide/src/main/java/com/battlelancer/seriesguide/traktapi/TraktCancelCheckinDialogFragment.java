package com.battlelancer.seriesguide.traktapi;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.format.DateUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.TraktTask.InitBundle;
import com.uwetrottmann.trakt5.services.Checkin;
import java.io.IOException;
import org.greenrobot.eventbus.EventBus;

/**
 * Warns about an ongoing check-in, how long it takes until it is finished. Offers to override or
 * wait out.
 */
public class TraktCancelCheckinDialogFragment extends AppCompatDialogFragment {

    private int waitTimeMinutes;

    /**
     * @param waitInMinutes The time to wait. If negative, will show as no time available.
     */
    static TraktCancelCheckinDialogFragment newInstance(Bundle traktTaskData,
            int waitInMinutes) {
        TraktCancelCheckinDialogFragment f = new TraktCancelCheckinDialogFragment();
        f.setArguments(traktTaskData);
        f.waitTimeMinutes = waitInMinutes;
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity().getApplicationContext();
        final Bundle args = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(context.getString(R.string.traktcheckin_inprogress,
                waitTimeMinutes < 0 ? context.getString(R.string.not_available)
                        : DateUtils.formatElapsedTime(waitTimeMinutes)));

        builder.setPositiveButton(R.string.traktcheckin_cancel, new OnClickListener() {

            @Override
            @SuppressLint("CommitTransaction")
            public void onClick(DialogInterface dialog, int which) {
                AsyncTask<String, Void, String> cancelCheckinTask
                        = new AsyncTask<String, Void, String>() {

                    @Override
                    protected String doInBackground(String... params) {
                        // check for credentials
                        if (!TraktCredentials.get(context).hasCredentials()) {
                            return context.getString(R.string.trakt_error_credentials);
                        }

                        Checkin checkin = SgApp.getServicesComponent(getContext()).traktCheckin();
                        try {
                            retrofit2.Response<Void> response = checkin.deleteActiveCheckin()
                                    .execute();
                            if (response.isSuccessful()) {
                                return null;
                            } else {
                                if (SgTrakt.isUnauthorized(context, response)) {
                                    return context.getString(R.string.trakt_error_credentials);
                                }
                                SgTrakt.trackFailedRequest(context, "delete check-in", response);
                            }
                        } catch (IOException e) {
                            SgTrakt.trackFailedRequest(context, "delete check-in", e);
                        }

                        return context.getString(R.string.api_error_generic,
                                context.getString(R.string.trakt));
                    }

                    @Override
                    protected void onPostExecute(String message) {
                        if (message == null) {
                            // all good
                            Toast.makeText(context, R.string.checkin_canceled_success_trakt,
                                    Toast.LENGTH_SHORT).show();

                            // relaunch the trakt task which called us to
                            // try the check in again
                            new TraktTask(context, args).executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                        } else {
                            // well, something went wrong
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                        }
                    }
                };

                cancelCheckinTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
