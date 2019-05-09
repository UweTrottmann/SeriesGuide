package com.battlelancer.seriesguide.ui.shows

import android.graphics.Canvas
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HeaderDecoration : RecyclerView.ItemDecoration() {

    // TODO recycle header views
    private val headers = SparseArray<View>()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val adapter = (parent.adapter as CalendarAdapter2)
        val position = parent.getChildLayoutPosition(view)
        if (position == RecyclerView.NO_POSITION
            || !adapter.itemHasHeader(position)) {
            headers.remove(position)
            return
        }

        val headerView = adapter.getHeaderView(parent, position)
        headers.put(position, headerView)

        measureHeaderView(headerView, parent)
        outRect.top = headerView.height
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION
                && (parent.adapter as CalendarAdapter2).itemHasHeader(position)) {
                canvas.save()
                val headerView = headers.get(position)
                canvas.translate(0f, child.y - headerView.height)
                headerView.draw(canvas)
                canvas.restore()
            }
        }
    }

    // TODO does not take into account item width
    private fun measureHeaderView(view: View, parent: ViewGroup) {
        if (view.layoutParams == null) {
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val displayMetrics = parent.context.resources.displayMetrics

        val widthSpec =
            View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY)
        val heightSpec =
            View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.EXACTLY)

        val childWidth = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.paddingLeft + parent.paddingRight, view.layoutParams.width
        )
        val childHeight = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom, view.layoutParams.height
        )

        view.measure(childWidth, childHeight)

        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

}