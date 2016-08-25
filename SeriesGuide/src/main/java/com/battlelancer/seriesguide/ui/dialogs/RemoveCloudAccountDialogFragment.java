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
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.backend.account.Account;
import de.greenrobot.event.EventBus;
import java.io.IOException;

/**
 * Confirms whether to obliterate a SeriesGuide cloud account.
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
                        Utils.executeInOrder(new RemoveHexagonAccountTask(getActivity()));
                    }
                }
        );
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    public static class RemoveHexagonAccountTask extends AsyncTask<Void, Void, Boolean> {

        public class HexagonAccountRemovedEvent {
            public final boolean successful;

            public HexagonAccountRemovedEvent(boolean successful) {
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

        private final Context mContext;

        public RemoveHexagonAccountTask(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // remove account from hexagon
            try {
                Account accountService = HexagonTools.buildAccountService(mContext);
                if (accountService == null) {
                    return false;
                }
                accountService.deleteData().execute();
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(mContext, "remove account", e);
                return false;
            }

            // sign out in SeriesGuide
            HexagonTools.storeAccountName(mContext, null);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            EventBus.getDefault().post(new HexagonAccountRemovedEvent(result));
        }
    }
}
