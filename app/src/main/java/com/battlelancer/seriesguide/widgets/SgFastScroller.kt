package com.battlelancer.seriesguide.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.StateListDrawable
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R

/**
 * Common setup for [FastScrollerDecoration] to keep its code close to AndroidX original
 * for easier patching.
 */
@SuppressLint("UseCompatLoadingForDrawables")
class SgFastScroller(context: Context, recyclerView: RecyclerView) {

    init {
        val thumbDrawable = context.getDrawable(R.drawable.fast_scroll_thumb) as StateListDrawable
        val trackDrawable = context.getDrawable(R.drawable.fast_scroll_track)
        val resources = context.resources
        FastScrollerDecoration(
            recyclerView, thumbDrawable, trackDrawable, thumbDrawable, trackDrawable,
            resources.getDimensionPixelSize(R.dimen.sg_fastscroll_default_thickness),
            resources.getDimensionPixelSize(R.dimen.sg_fastscroll_minimum_height),
            resources.getDimensionPixelSize(R.dimen.sg_fastscroll_minimum_range),
            resources.getDimensionPixelOffset(R.dimen.sg_fastscroll_margin)
        )
    }

}