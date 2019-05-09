package com.battlelancer.seriesguide.ui.shows

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HeaderDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val itemHasHeader = (parent.adapter as CalendarAdapter2).itemHasHeader(itemPosition)
        // item with header + all items in same row :/
        val topInset = if (itemHasHeader) {
            100
        } else {
            0
        }
        outRect.set(0, topInset, 0, 0)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
    }

}