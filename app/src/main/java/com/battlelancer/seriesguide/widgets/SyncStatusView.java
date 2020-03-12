package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.battlelancer.seriesguide.databinding.ViewSyncStatusBinding;
import com.battlelancer.seriesguide.sync.SyncProgress;

public class SyncStatusView extends LinearLayout {

    private ViewSyncStatusBinding binding;

    public SyncStatusView(Context context) {
        this(context, null);
    }

    public SyncStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        binding = ViewSyncStatusBinding.inflate(LayoutInflater.from(context), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding.imageViewSyncStatus.setVisibility(GONE);
    }

    /**
     * If there is progress or a failure result, displays it.
     * Otherwise sets the view {@link View#GONE}.
     */
    public void setProgress(SyncProgress.SyncEvent event) {
        if (event.isSyncing()) {
            binding.progressBarSyncStatus.setVisibility(View.VISIBLE);
            binding.imageViewSyncStatus.setVisibility(GONE);
            setVisibility(VISIBLE);
        } else {
            // Finished.
            binding.progressBarSyncStatus.setVisibility(View.GONE);

            if (event.isFinishedWithError()) {
                binding.imageViewSyncStatus.setVisibility(VISIBLE);
                setVisibility(VISIBLE);
            } else {
                // Successful.
                binding.imageViewSyncStatus.setVisibility(GONE);
                setVisibility(GONE);
                return; // No need to update status text.
            }
        }
        binding.textViewSyncStatus.setText(event.getDescription(getContext()));
    }
}
