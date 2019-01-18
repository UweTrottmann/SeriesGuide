package com.battlelancer.seriesguide.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager

/**
 * [ViewPager] that measures current page height to use as its height.
 */
class WrappingViewPager @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    init {
        addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                requestLayout() // to measure new current page and adjust height
            }

        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpecUpdate = heightMeasureSpec

        val child = getChildAt(currentItem)
        if (child != null) {
            child.measure(
                widthMeasureSpec,
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val h = child.measuredHeight
            heightMeasureSpecUpdate = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpecUpdate)
    }

}