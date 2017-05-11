package com.battlelancer.seriesguide.extensions;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import java.util.List;
import timber.log.Timber;

public class ActionsHelper {

    /**
     * Replaces all child views of the given {@link android.view.ViewGroup} with a {@link
     * android.widget.Button} per action plus one linking to {@link com.battlelancer.seriesguide.extensions.ExtensionsConfigurationActivity}.
     * Sets up {@link android.view.View.OnClickListener} if {@link com.battlelancer.seriesguide.api.Action#getViewIntent()}
     * of an  {@link com.battlelancer.seriesguide.api.Action} is not null.
     */
    public static void populateActions(@NonNull LayoutInflater layoutInflater,
            @NonNull Resources.Theme theme, @Nullable ViewGroup actionsContainer,
            @Nullable List<Action> data, @NonNull final String logCategory) {
        if (actionsContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateActions: action view container gone, aborting");
            return;
        }
        actionsContainer.removeAllViews();

        // re-use drawable for all buttons
        VectorDrawableCompat drawable = ViewTools.createVectorIcon(actionsContainer.getContext(),
                theme, R.drawable.ic_extension_black_24dp);

        // add a view per action
        if (data != null) {
            for (Action action : data) {
                Button actionView = (Button) layoutInflater.inflate(R.layout.item_action,
                        actionsContainer, false);
                actionView.setText(action.getTitle());
                ViewTools.setCompoundDrawablesRelativeWithIntrinsicBounds(actionView, drawable,
                        null, null, null);

                CheatSheet.setup(actionView, action.getTitle());

                final Intent viewIntent = action.getViewIntent();
                if (viewIntent != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    } else {
                        //noinspection deprecation
                        viewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    }
                    actionView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Utils.tryStartActivity(v.getContext(), viewIntent, true);
                        }
                    });
                }

                actionsContainer.addView(actionView);
            }
        }

        // link to extensions configuration
        TextView configureView = (TextView) layoutInflater.inflate(R.layout.item_action_add,
                actionsContainer, false);
        configureView.setText(R.string.action_extensions_configure);
        configureView.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ExtensionsConfigurationActivity.class);
                if (AndroidUtils.isJellyBeanOrHigher()) {
                    v.getContext()
                            .startActivity(intent,
                                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(),
                                            v.getHeight()).toBundle());
                } else {
                    v.getContext().startActivity(intent);
                }
                Utils.trackAction(v.getContext(), logCategory, "Manage extensions");
            }
        });
        actionsContainer.addView(configureView);
    }
}
