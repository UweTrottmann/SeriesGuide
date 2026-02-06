// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ViewFeatureStatusBinding;

/**
 * A visual indicator and description of a feature that can have one of {@link FeatureState}.
 */
public class FeatureStatusView extends LinearLayout {

    public enum FeatureState {
        // The values must match the featureState enum declared in attrs.xml
        NOT_SUPPORTED(0),
        SUPPORTED(1),
        DISABLED(2);

        private final int value;

        FeatureState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private final int featureState;
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
            featureState = a.getInt(R.styleable.FeatureStatusView_featureState,
                    FeatureState.SUPPORTED.getValue());
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
        updateFeatureState(featureState);
        binding.textViewStatus.setText(featureDescription);
    }

    private void updateFeatureState(int state) {
        int iconRes;
        int contentDescriptionRes;

        if (state == FeatureState.DISABLED.getValue()) {
            iconRes = R.drawable.ic_cancel_red_24dp;
            contentDescriptionRes = R.string.feature_disabled;
        } else if (state == FeatureState.NOT_SUPPORTED.getValue()) {
            iconRes = R.drawable.ic_remove_circle_black_24dp;
            contentDescriptionRes = R.string.feature_not_supported;
        } else {
            iconRes = R.drawable.ic_check_circle_green_24dp;
            contentDescriptionRes = R.string.feature_supported;
        }

        VectorDrawableCompat drawable = VectorDrawableCompat.create(
                getContext().getResources(), iconRes, getContext().getTheme());
        binding.imageViewStatus.setImageDrawable(drawable);
        binding.imageViewStatus.setContentDescription(
                getContext().getString(contentDescriptionRes));
    }

    public void setFeatureState(FeatureState state) {
        updateFeatureState(state.getValue());
    }
}
