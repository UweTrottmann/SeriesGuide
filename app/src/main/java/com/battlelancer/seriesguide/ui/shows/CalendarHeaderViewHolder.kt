package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.TimeTools
import java.util.Date

class CalendarHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val headerTextView: TextView = itemView as TextView

    fun bind(context: Context, headerTime: Long) {
        // display headers like "Mon in 3 days", also "today" when applicable
        headerTextView.text = TimeTools.formatToLocalDayAndRelativeWeek(context, Date(headerTime))
    }

    companion object {

        fun create(parent: ViewGroup): CalendarHeaderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_grid_header, parent, false)
            return CalendarHeaderViewHolder(view)
        }

    }

}
