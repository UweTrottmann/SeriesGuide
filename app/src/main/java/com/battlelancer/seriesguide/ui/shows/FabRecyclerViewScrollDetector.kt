package com.battlelancer.seriesguide.ui.shows

import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

/**
 * Hides the floating action button when scrolling down, shows it when scrolling up. If the view can
 * no longer scroll down, shows the button also.
 *
 * Built upon https://github.com/makovkastar/FloatingActionButton scroll detectors.
 */
internal class FabRecyclerViewScrollDetector(private val button: FloatingActionButton) :
    RecyclerView.OnScrollListener() {

    private val scrollThreshold: Int =
        button.context.resources.getDimensionPixelOffset(R.dimen.fab_scroll_threshold)

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        // always show if scrolled to bottom
        if (!recyclerView.canScrollVertically(1 /* down */)) {
            button.show()
            return
        }

        val isSignificantDelta = abs(dy) > scrollThreshold
        if (isSignificantDelta) {
            if (dy > 0) {
                onScrollDown()
            } else {
                onScrollUp()
            }
        }
    }

    private fun onScrollDown() {
        button.hide()
    }

    private fun onScrollUp() {
        button.show()
    }

}
