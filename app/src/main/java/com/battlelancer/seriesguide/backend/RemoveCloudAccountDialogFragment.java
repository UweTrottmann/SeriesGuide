package com.battlelancer.seriesguide.backend;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
                (dialog, which) -> Utils.executeInOrder(new RemoveHexagonAccountTask(getContext()))
        );
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> sendCanceledEvent());

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

        @SuppressLint("StaticFieldLeak") private final Context context;
        private final HexagonTools hexagonTools;

        public RemoveHexagonAccountTask(Context context) {
            this.context = context.getApplicationContext();
            this.hexagonTools = SgApp.getServicesComponent(context).hexagonTools();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // remove account from hexagon
            try {
                Account accountService = hexagonTools.buildAccountService();
                if (accountService == null) {
                    return false;
                }
                accountService.deleteData().execute();
            } catch (IOException e) {
                Errors.logAndReportHexagon("remove account", e);
                return false;
            }

            // de-authorize app so other clients are signed out as well
            GoogleSignInClient googleSignInClient = GoogleSignIn
                    .getClient(context, HexagonTools.getGoogleSignInOptions());
            Task<Void> task = googleSignInClient.revokeAccess();
            try {
                Tasks.await(task);
            } catch (Exception e) {
                Errors.logAndReport("remove account", HexagonAuthError.build("remove account", e));
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
