package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2ViewModel.CalendarItem
import com.battlelancer.seriesguide.util.TimeTools
import java.util.Date

class CalendarHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val headerTextView: TextView = itemView as TextView

    fun bind(context: Context, item: CalendarItem?) {
        headerTextView.text = if (item != null) {
            // display headers like "Mon in 3 days", also "today" when applicable
            TimeTools.formatToLocalDayAndRelativeWeek(context, Date(item.headerTime))
        } else {
            null
        }
    }

    companion object {

        fun create(parent: ViewGroup): CalendarHeaderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_grid_header, parent, false)
            return CalendarHeaderViewHolder(view)
        }

    }

}
