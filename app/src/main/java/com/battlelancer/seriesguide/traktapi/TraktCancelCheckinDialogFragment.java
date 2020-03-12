package com.battlelancer.seriesguide.traktapi;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.TraktTask.InitBundle;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.Errors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uwetrottmann.trakt5.services.Checkin;
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
    static void show(FragmentManager fragmentManager, Bundle traktTaskData, int waitInMinutes) {
        TraktCancelCheckinDialogFragment f = new TraktCancelCheckinDialogFragment();
        f.setArguments(traktTaskData);
        f.waitTimeMinutes = waitInMinutes;
        DialogTools.safeShow(f, fragmentManager, "cancel-checkin-dialog");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();
        if (args == null) {
            throw new IllegalArgumentException("Missing args");
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        builder.setMessage(getString(R.string.traktcheckin_inprogress,
                waitTimeMinutes < 0 ? getString(R.string.not_available)
                        : DateUtils.formatElapsedTime(waitTimeMinutes)));

        builder.setPositiveButton(R.string.traktcheckin_cancel, (dialog, which) -> {
            AsyncTask<String, Void, String> cancelCheckinTask
                    = new CancelCheckInTask(requireContext(), args);
            cancelCheckinTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
        builder.setNegativeButton(R.string.traktcheckin_wait, (dialog, which) -> {
            // broadcast check-in success
            EventBus.getDefault().post(new TraktTask.TraktActionCompleteEvent(
                    TraktAction.valueOf(args.getString(InitBundle.TRAKTACTION)), true, null));
        });

        return builder.create();
    }

    private static class CancelCheckInTask extends AsyncTask<String, Void, String> {

        @SuppressLint("StaticFieldLeak") private final Context context;
        private final Bundle traktTaskArgs;

        CancelCheckInTask(@NonNull Context context, @NonNull Bundle traktTaskArgs) {
            this.context = context.getApplicationContext();
            this.traktTaskArgs = traktTaskArgs;
        }

        @Override
        protected String doInBackground(String... params) {
            // check for credentials
            if (!TraktCredentials.get(context).hasCredentials()) {
                return context.getString(R.string.trakt_error_credentials);
            }

            Checkin checkin = SgApp.getServicesComponent(context).trakt().checkin();
            try {
                retrofit2.Response<Void> response = checkin.deleteActiveCheckin().execute();
                if (response.isSuccessful()) {
                    return null;
                } else {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return context.getString(R.string.trakt_error_credentials);
                    }
                    Errors.logAndReport("delete check-in", response);
                }
            } catch (Exception e) {
                Errors.logAndReport("delete check-in", e);
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
                new TraktTask(context, traktTaskArgs)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                // well, something went wrong
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }
    }
}
