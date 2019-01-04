package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.StringRes;
import com.battlelancer.seriesguide.R;

/**
 * Helper layout to show an empty view with a message and action button (e.g. to refresh content).
 */
public class EmptyView extends FrameLayout {

    private final TextView emptyViewText;
    private final Button emptyViewButton;

    public EmptyView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.empty_view, this, true);
        emptyViewText = findViewById(R.id.textViewEmptyView);
        emptyViewButton = findViewById(R.id.buttonEmptyView);

        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.EmptyView, 0, 0);

        try {
            emptyViewText.setText(a.getString(R.styleable.EmptyView_emptyViewMessage));
            emptyViewButton.setText(a.getString(R.styleable.EmptyView_emptyViewButtonText));
        } finally {
            a.recycle();
        }
    }

    public void setMessage(@StringRes int textResId) {
        emptyViewText.setText(textResId);
    }

    public void setMessage(CharSequence textResId) {
        emptyViewText.setText(textResId);
    }

    public void setButtonText(@StringRes int textResId) {
        emptyViewButton.setText(textResId);
    }

    public void setButtonClickListener(OnClickListener listener) {
        emptyViewButton.setOnClickListener(listener);
    }

    public void setContentVisibility(int visibility) {
        emptyViewText.setVisibility(visibility);
        emptyViewButton.setVisibility(visibility);
    }
}
