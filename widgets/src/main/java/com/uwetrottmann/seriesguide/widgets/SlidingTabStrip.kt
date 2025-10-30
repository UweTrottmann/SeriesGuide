// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann
// Copyright 2013 The Android Open Source Project

package com.uwetrottmann.seriesguide.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.LinearLayout

class SlidingTabStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    /**
     * See [setDisplayUnderline].
     */
    private var displayUnderline = false
    private val underlineThickness: Int
    private val underlinePaint: Paint

    private val selectedIndicatorThickness: Int
    private val selectedIndicatorPaint: Paint

    private var selectedPosition = 0
    private var selectionOffset = 0f

    private var tabColorizerCustom: SlidingTabLayout.TabColorizer? = null
    private val tabColorizerDefault: SimpleTabColorizer

    init {
        setWillNotDraw(false)

        tabColorizerDefault = SimpleTabColorizer()
        tabColorizerDefault.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR)

        underlineThickness = resources
            .getDimensionPixelSize(R.dimen.sg_sliding_tab_strip_underline_size)
        underlinePaint = Paint()
        underlinePaint.color = DEFAULT_UNDERLINE_COLOR

        selectedIndicatorThickness = resources
            .getDimensionPixelSize(R.dimen.sg_sliding_tab_strip_indicator_size)
        selectedIndicatorPaint = Paint()
    }

    fun setCustomTabColorizer(tabColorizer: SlidingTabLayout.TabColorizer?) {
        this.tabColorizerCustom = tabColorizer
        invalidate()
    }

    fun setSelectedIndicatorColors(vararg colors: Int) {
        // Make sure that the custom colorizer is removed
        tabColorizerCustom = null
        tabColorizerDefault.setIndicatorColors(*colors)
        invalidate()
    }

    /**
     * Whether to draw an underline below all tabs.
     */
    fun setDisplayUnderline(displayUnderline: Boolean) {
        this.displayUnderline = displayUnderline
        invalidate()
    }

    fun setUnderlineColor(color: Int) {
        underlinePaint.color = color
        invalidate()
    }

    fun onViewPagerPageChanged(position: Int, positionOffset: Float) {
        selectedPosition = position
        selectionOffset = positionOffset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val height = height
        val childCount = childCount
        val tabColorizer = tabColorizerCustom ?: tabColorizerDefault

        if (displayUnderline) {
            // Colored underline below all tabs
            canvas.drawRect(
                0f,
                (height - underlineThickness).toFloat(),
                width.toFloat(),
                height.toFloat(),
                underlinePaint
            )
        }

        // Colored underline below the current selection
        if (childCount > 0) {
            val selectedTitle = getChildAt(selectedPosition)
            var left = selectedTitle.left
            var right = selectedTitle.right
            var color = tabColorizer.getIndicatorColor(selectedPosition)

            if (selectionOffset > 0f && selectedPosition < childCount - 1) {
                val nextColor = tabColorizer.getIndicatorColor(selectedPosition + 1)
                if (color != nextColor) {
                    color = blendColors(nextColor, color, selectionOffset)
                }

                // Draw the selection partway between the tabs
                val nextTitle = getChildAt(selectedPosition + 1)
                left = (selectionOffset * nextTitle.left +
                        (1.0f - selectionOffset) * left).toInt()
                right = (selectionOffset * nextTitle.right +
                        (1.0f - selectionOffset) * right).toInt()
            }

            selectedIndicatorPaint.color = color

            if (displayUnderline) {
                canvas.drawRect(
                    left.toFloat(),
                    (height - selectedIndicatorThickness - underlineThickness).toFloat(),
                    right.toFloat(),
                    (height - underlineThickness).toFloat(),
                    selectedIndicatorPaint
                )
            } else {
                canvas.drawRect(
                    left.toFloat(),
                    (height - selectedIndicatorThickness).toFloat(),
                    right.toFloat(),
                    height.toFloat(), selectedIndicatorPaint
                )
            }
        }
    }

    private class SimpleTabColorizer : SlidingTabLayout.TabColorizer {
        private var indicatorColors: IntArray = IntArray(0)

        override fun getIndicatorColor(position: Int): Int {
            return indicatorColors[position % indicatorColors.size]
        }

        fun setIndicatorColors(vararg colors: Int) {
            indicatorColors = colors
        }
    }

    companion object {
        private const val DEFAULT_SELECTED_INDICATOR_COLOR = 0xFF33B5E5.toInt()
        private const val DEFAULT_UNDERLINE_COLOR = 0x1A000000

        /**
         * Set the alpha value of the [color] to be the given [alpha] value.
         */
        private fun setColorAlpha(color: Int, alpha: Byte): Int {
            return Color.argb(
                alpha.toInt(),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }

        /**
         * Blend [color1] and [color2] using the given ratio.
         *
         * @param ratio of which to blend. 1.0 will return [color1], 0.5 will give an even blend,
         *              0.0 will return [color2].
         */
        private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRation = 1f - ratio
            val r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation)
            val g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation)
            val b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation)
            return Color.rgb(r.toInt(), g.toInt(), b.toInt())
        }
    }
}