package com.battlelancer.seriesguide.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;
import java.util.Locale;

/**
 * Receives feedback intent from chrome custom tab.
 */
public class FeedbackBroadcastReceiver extends BroadcastReceiver {

    private static final String SUPPORT_MAIL = "support@seriesgui.de";

    public static Intent getFeedbackEmailIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {SUPPORT_MAIL});
        // include app version in subject
        intent.putExtra(Intent.EXTRA_SUBJECT,
                "SeriesGuide " + Utils.getVersion(context) + " Feedback");
        // and hardware and Android info in body
        intent.putExtra(Intent.EXTRA_TEXT,
                Build.MANUFACTURER.toUpperCase(Locale.US) + " " + Build.MODEL + ", Android "
                        + Build.VERSION.RELEASE + "\n\n");

        return Intent.createChooser(intent, context.getString(R.string.feedback));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent chooserIntent = getFeedbackEmailIntent(context);
        // need to set new task flag, as this is executed from a custom tab
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }

}
