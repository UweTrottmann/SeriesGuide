/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.uwetrottmann.seriesguide.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

class SlidingTabStrip extends LinearLayout {

    private static final int DEFAULT_SELECTED_INDICATOR_COLOR = 0xFF33B5E5;
    private static final int DEFAULT_UNDERLINE_COLOR = 0x1A000000;

    private boolean displayUnderline;
    private final int underlineThickness;
    private final Paint underlinePaint;

    private final int selectedIndicatorThickness;
    private final Paint selectedIndicatorPaint;

    private int selectedPosition;
    private float selectionOffset;

    private SlidingTabLayout.TabColorizer tabColorizerCustom;
    private final SimpleTabColorizer tabColorizerDefault;

    SlidingTabStrip(Context context) {
        this(context, null);
    }

    SlidingTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        tabColorizerDefault = new SimpleTabColorizer();
        tabColorizerDefault.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR);

        underlineThickness = getResources()
                .getDimensionPixelSize(R.dimen.sg_sliding_tab_strip_underline_size);
        underlinePaint = new Paint();
        underlinePaint.setColor(DEFAULT_UNDERLINE_COLOR);

        selectedIndicatorThickness = getResources()
                .getDimensionPixelSize(R.dimen.sg_sliding_tab_strip_indicator_size);
        selectedIndicatorPaint = new Paint();
    }

    void setCustomTabColorizer(SlidingTabLayout.TabColorizer tabColorizer) {
        this.tabColorizerCustom = tabColorizer;
        invalidate();
    }

    void setSelectedIndicatorColors(int... colors) {
        // Make sure that the custom colorizer is removed
        tabColorizerCustom = null;
        tabColorizerDefault.setIndicatorColors(colors);
        invalidate();
    }

    void setDisplayUnderline(boolean displayUnderline) {
        this.displayUnderline = displayUnderline;
        invalidate();
    }

    void setUnderlineColor(int color) {
        underlinePaint.setColor(color);
        invalidate();
    }

    void onViewPagerPageChanged(int position, float positionOffset) {
        selectedPosition = position;
        selectionOffset = positionOffset;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int height = getHeight();
        final int childCount = getChildCount();
        final SlidingTabLayout.TabColorizer tabColorizer = tabColorizerCustom != null
                ? tabColorizerCustom
                : tabColorizerDefault;

        if (displayUnderline) {
            // Colored underline below all tabs
            canvas.drawRect(0, height - underlineThickness, getWidth(), height, underlinePaint);
        }

        // Colored underline below the current selection
        if (childCount > 0) {
            View selectedTitle = getChildAt(selectedPosition);
            int left = selectedTitle.getLeft();
            int right = selectedTitle.getRight();
            int color = tabColorizer.getIndicatorColor(selectedPosition);

            if (selectionOffset > 0f && selectedPosition < (getChildCount() - 1)) {
                int nextColor = tabColorizer.getIndicatorColor(selectedPosition + 1);
                if (color != nextColor) {
                    color = blendColors(nextColor, color, selectionOffset);
                }

                // Draw the selection partway between the tabs
                View nextTitle = getChildAt(selectedPosition + 1);
                left = (int) (selectionOffset * nextTitle.getLeft() +
                        (1.0f - selectionOffset) * left);
                right = (int) (selectionOffset * nextTitle.getRight() +
                        (1.0f - selectionOffset) * right);
            }

            selectedIndicatorPaint.setColor(color);

            if (displayUnderline) {
                canvas.drawRect(left, height - selectedIndicatorThickness - underlineThickness,
                        right, height - underlineThickness, selectedIndicatorPaint);
            } else {
                canvas.drawRect(left, height - selectedIndicatorThickness,
                        right, height, selectedIndicatorPaint);
            }
        }
    }

    /**
     * Set the alpha value of the {@code color} to be the given {@code alpha} value.
     */
    private static int setColorAlpha(int color, byte alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Blend {@code color1} and {@code color2} using the given ratio.
     *
     * @param ratio of which to blend. 1.0 will return {@code color1}, 0.5 will give an even blend,
     *              0.0 will return {@code color2}.
     */
    private static int blendColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private static class SimpleTabColorizer implements SlidingTabLayout.TabColorizer {
        private int[] indicatorColors;

        @Override
        public final int getIndicatorColor(int position) {
            return indicatorColors[position % indicatorColors.length];
        }

        void setIndicatorColors(int... colors) {
            indicatorColors = colors;
        }
    }
}