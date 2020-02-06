package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;

public class FeatureStatusView extends LinearLayout {

    @BindView(R.id.imageViewStatus) ImageView imageViewStatus;
    @BindView(R.id.textViewStatus) TextView textViewStatus;

    private final boolean featureSupported;
    private final String featureDescription;

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
        LayoutInflater.from(context).inflate(R.layout.view_feature_status, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
        if (featureSupported) {
            setFeatureEnabled(true);
        } else {
            imageViewStatus.setImageResource(R.drawable.ic_remove_circle_black_24dp);
            imageViewStatus.setContentDescription(
                    getContext().getString(R.string.feature_not_supported));
        }
        textViewStatus.setText(featureDescription);
    }

    public void setFeatureEnabled(boolean available) {
        if (featureSupported) {
            VectorDrawableCompat drawable = VectorDrawableCompat.create(getContext().getResources(),
                    available
                            ? R.drawable.ic_check_circle_green_24dp
                            : R.drawable.ic_cancel_red_24dp,
                    getContext().getTheme());
            imageViewStatus.setImageDrawable(drawable);
            imageViewStatus.setContentDescription(getContext().getString(available
                    ? R.string.feature_supported
                    : R.string.feature_not_supported));
        }
    }
}
