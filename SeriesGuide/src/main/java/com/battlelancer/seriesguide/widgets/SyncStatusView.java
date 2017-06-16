package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.sync.SyncProgress;

public class SyncStatusView extends LinearLayout {

    @BindView(R.id.progressBarSyncStatus) ProgressBar progressBar;
    @BindView(R.id.imageViewSyncStatus) ImageView imageView;
    @BindView(R.id.textViewSyncStatus) TextView textView;

    public SyncStatusView(Context context) {
        this(context, null);
    }

    public SyncStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.view_sync_status, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        imageView.setVisibility(GONE);
    }

    /**
     * If there is progress or a failure result, displays it. Otherwise sets the view {@link
     * View#GONE}.
     */
    public void setProgress(SyncProgress.SyncEvent event) {
        if (event.result == null) {
            progressBar.setVisibility(View.VISIBLE);
            imageView.setVisibility(GONE);
            setVisibility(VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            if (event.result == SyncProgress.Result.FAILURE) {
                imageView.setVisibility(VISIBLE);
                setVisibility(VISIBLE);
            } else {
                imageView.setVisibility(GONE);
                setVisibility(GONE);
                return; // no need to update status text
            }
        }

        StringBuilder statusText = new StringBuilder(
                getContext().getString(R.string.sync_and_update));
        if (event.step != null) {
            statusText.append(" - ");
            statusText.append(event.step.description);
        }
        textView.setText(statusText);
    }
}
