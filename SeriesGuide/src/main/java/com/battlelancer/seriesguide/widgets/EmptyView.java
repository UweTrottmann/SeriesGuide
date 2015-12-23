/*
 * Copyright 2015 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
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
        emptyViewText = (TextView) findViewById(R.id.textViewEmptyView);
        emptyViewButton = (Button) findViewById(R.id.buttonEmptyView);

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
