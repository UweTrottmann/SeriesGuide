package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;

public class AddIndicator extends FrameLayout {

    public static final int STATE_ADD = 0;
    public static final int STATE_ADDING = 1;
    public static final int STATE_ADDED = 2;

    @BindView(R.id.imageViewAddIndicator) ImageView add;
    @BindView(R.id.imageViewAddIndicatorAdded) ImageView added;
    @BindView(R.id.progressBarAddIndicator) ProgressBar progressBar;

    public AddIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.layout_add_indicator, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);

        added.setVisibility(GONE);
        progressBar.setVisibility(GONE);
    }

    public void setContentDescriptionAdded(CharSequence contentDescription) {
        added.setContentDescription(contentDescription);
    }

    public void setOnAddClickListener(OnClickListener onClickListener) {
        add.setOnClickListener(onClickListener);
    }

    public void setState(int state) {
        if (state == STATE_ADD) {
            add.setVisibility(VISIBLE);
            progressBar.setVisibility(GONE);
            added.setVisibility(GONE);
        } else if (state == STATE_ADDING) {
            add.setVisibility(GONE);
            progressBar.setVisibility(VISIBLE);
            added.setVisibility(GONE);
        } else if (state == STATE_ADDED) {
            add.setVisibility(GONE);
            progressBar.setVisibility(GONE);
            added.setVisibility(VISIBLE);
        }
    }

}
