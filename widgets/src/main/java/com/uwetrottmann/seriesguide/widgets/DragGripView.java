/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uwetrottmann.seriesguide.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

/**
 * From https://code.google.com/p/dashclock. Modified for compatibility with API levels below 17.
 */
public class DragGripView extends View {
    private static final int[] ATTRS = new int[] {
            android.R.attr.gravity,
            android.R.attr.color,
    };

    private static final int HORIZ_RIDGES = 2;

    private int gravity = Gravity.START;
    private int color = 0x33333333;

    private Paint ridgePaint;
    private RectF tempRectF = new RectF();

    private float tidgeSize;
    private float ridgeGap;

    private int width;
    private int height;

    public DragGripView(Context context) {
        this(context, null, 0);
    }

    public DragGripView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragGripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        gravity = a.getInteger(0, gravity);
        //noinspection ResourceType
        color = a.getColor(1, color);
        a.recycle();

        final Resources res = getResources();
        tidgeSize = res.getDimensionPixelSize(R.dimen.drag_grip_ridge_size);
        ridgeGap = res.getDimensionPixelSize(R.dimen.drag_grip_ridge_gap);

        ridgePaint = new Paint();
        ridgePaint.setColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                View.resolveSize(
                        (int) (HORIZ_RIDGES * (tidgeSize + ridgeGap) - ridgeGap)
                                + getPaddingLeft() + getPaddingRight(),
                        widthMeasureSpec
                ),
                View.resolveSize(
                        (int) tidgeSize,
                        heightMeasureSpec)
        );
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float drawWidth = HORIZ_RIDGES * (tidgeSize + ridgeGap) - ridgeGap;
        float drawLeft;

        switch (Gravity.getAbsoluteGravity(gravity, getLayoutDirection())
                & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.CENTER_HORIZONTAL:
                drawLeft = getPaddingLeft()
                        + ((width - getPaddingLeft() - getPaddingRight()) - drawWidth) / 2;
                break;
            case Gravity.RIGHT:
                drawLeft = getWidth() - getPaddingRight() - drawWidth;
                break;
            default:
                drawLeft = getPaddingLeft();
        }

        int vertRidges = (int) ((height - getPaddingTop() - getPaddingBottom() + ridgeGap)
                / (tidgeSize + ridgeGap));
        float drawHeight = vertRidges * (tidgeSize + ridgeGap) - ridgeGap;
        float drawTop = getPaddingTop()
                + ((height - getPaddingTop() - getPaddingBottom()) - drawHeight) / 2;

        for (int y = 0; y < vertRidges; y++) {
            for (int x = 0; x < HORIZ_RIDGES; x++) {
                tempRectF.set(
                        drawLeft + x * (tidgeSize + ridgeGap),
                        drawTop + y * (tidgeSize + ridgeGap),
                        drawLeft + x * (tidgeSize + ridgeGap) + tidgeSize,
                        drawTop + y * (tidgeSize + ridgeGap) + tidgeSize);
                canvas.drawOval(tempRectF, ridgePaint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
        width = w;
    }
}
