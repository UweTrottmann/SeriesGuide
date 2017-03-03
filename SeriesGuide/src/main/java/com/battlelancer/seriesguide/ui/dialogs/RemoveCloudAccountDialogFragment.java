package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.uwetrottmann.seriesguide.backend.account.Account;
import java.io.IOException;
import org.greenrobot.eventbus.EventBus;

/**
 * Confirms whether to obliterate a SeriesGuide cloud account. If removal is tried, posts result as
 * {@link AccountRemovedEvent}. If dialog is canceled, posts a {@link CanceledEvent}.
 */
public class RemoveCloudAccountDialogFragment extends AppCompatDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.hexagon_remove_account_confirmation);
        builder.setPositiveButton(R.string.hexagon_remove_account,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.executeInOrder(
                                new RemoveHexagonAccountTask(SgApp.from(getActivity())));
                    }
                }
        );
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendCanceledEvent();
            }
        });

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        sendCanceledEvent();
    }

    private void sendCanceledEvent() {
        EventBus.getDefault().post(new CanceledEvent());
    }

    public static class RemoveHexagonAccountTask extends AsyncTask<Void, Void, Boolean> {

        private final SgApp app;

        public RemoveHexagonAccountTask(SgApp app) {
            this.app = app;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // remove account from hexagon
            HexagonTools hexagonTools = app.getHexagonTools();
            try {
                Account accountService = hexagonTools.buildAccountService();
                if (accountService == null) {
                    return false;
                }
                accountService.deleteData().execute();
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(app, "remove account", e);
                return false;
            }

            // de-authorize app so other clients are signed out as well
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(app)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, HexagonTools.getGoogleSignInOptions())
                    .build();
            ConnectionResult connectionResult = googleApiClient.blockingConnect();
            if (!connectionResult.isSuccess()) {
                return false;
            }
            com.google.android.gms.common.api.Status status = Auth.GoogleSignInApi
                    .revokeAccess(googleApiClient).await();
            if (!status.isSuccess()) {
                return false;
            }

            // disable Hexagon integration, remove local account data
            hexagonTools.setDisabled();

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            EventBus.getDefault().post(new AccountRemovedEvent(result));
        }
    }

    public static class CanceledEvent {
    }

    public static class AccountRemovedEvent {
        public final boolean successful;

        public AccountRemovedEvent(boolean successful) {
            this.successful = successful;
        }

        /**
         * Display status toasts depending on the result.
         */
        public void handle(Context context) {
            Toast.makeText(context, successful ? R.string.hexagon_remove_account_success
                    : R.string.hexagon_remove_account_failure, Toast.LENGTH_LONG).show();
        }
    }
}
