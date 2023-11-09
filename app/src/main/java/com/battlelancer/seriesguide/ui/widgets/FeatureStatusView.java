// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ViewFeatureStatusBinding;

public class FeatureStatusView extends LinearLayout {

    private final boolean featureSupported;
    private final String featureDescription;
    private final ViewFeatureStatusBinding binding;

    public FeatureStatusView(Context context) {
        this(context, null);
    }

    public FeatureStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.FeatureStatusView, 0, 0);
        try {
            featureSupported = a.getBoolean(R.styleable.FeatureStatusView_featureSupported, true);
            featureDescription = a.getString(R.styleable.FeatureStatusView_featureDescription);
        } finally {
            a.recycle();
        }

        setOrientation(HORIZONTAL);
        binding = ViewFeatureStatusBinding.inflate(LayoutInflater.from(context), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (featureSupported) {
            setFeatureEnabled(true);
        } else {
            binding.imageViewStatus.setImageResource(R.drawable.ic_remove_circle_black_24dp);
            binding.imageViewStatus.setContentDescription(
                    getContext().getString(R.string.feature_not_supported));
        }
        binding.textViewStatus.setText(featureDescription);
    }

    public void setFeatureEnabled(boolean available) {
        if (featureSupported) {
            VectorDrawableCompat drawable = VectorDrawableCompat.create(getContext().getResources(),
                    available
                            ? R.drawable.ic_check_circle_green_24dp
                            : R.drawable.ic_cancel_red_24dp,
                    getContext().getTheme());
            binding.imageViewStatus.setImageDrawable(drawable);
            binding.imageViewStatus.setContentDescription(getContext().getString(available
                    ? R.string.feature_supported
                    : R.string.feature_not_supported));
        }
    }
}
