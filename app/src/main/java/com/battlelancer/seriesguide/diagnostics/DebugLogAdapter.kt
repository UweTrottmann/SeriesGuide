// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemDebugLogBinding

/**
 * Displays a list of [DebugLogEntry].
 */
class DebugLogAdapter : RecyclerView.Adapter<DebugLogViewHolder>() {

    private val debugLogEntries = mutableListOf<DebugLogEntry>()

    override fun getItemCount(): Int = debugLogEntries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebugLogViewHolder {
        return DebugLogViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: DebugLogViewHolder, position: Int) {
        val item = debugLogEntries.getOrNull(position)
        holder.bindTo(item)
    }

    @SuppressLint("NotifyDataSetChanged") // Don't care about individual item change tracking
    fun setData(items: List<DebugLogEntry>) {
        debugLogEntries.clear()
        debugLogEntries.addAll(items)
        notifyDataSetChanged()
    }

}

class DebugLogViewHolder(
    private val binding: ItemDebugLogBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(entry: DebugLogEntry?) {
        if (entry == null) {
            binding.textViewItemDebugLogLevel.text = ""
            binding.textViewItemDebugLogTag.text = ""
            binding.textViewItemDebugLogMessage.text = ""
        } else {
            binding.textViewItemDebugLogLevel.apply {
                setBackgroundResource(backgroundForLevel(entry.level))
                text = entry.displayLevel()
            }
            binding.textViewItemDebugLogTag.text =
                String.format("%s %s", entry.timeStamp, entry.tag)
            binding.textViewItemDebugLogMessage.text = entry.message
        }
    }

    @ColorRes
    private fun backgroundForLevel(level: Int): Int {
        return when (level) {
            Log.VERBOSE, Log.DEBUG -> R.color.sg_debuglog_debug
            Log.INFO -> R.color.sg_debuglog_info
            Log.WARN -> R.color.sg_debuglog_warn
            Log.ERROR, Log.ASSERT -> R.color.sg_debuglog_error
            else -> R.color.sg_debuglog_unknown
        }
    }

    companion object {
        fun inflate(parent: ViewGroup) =
            DebugLogViewHolder(
                ItemDebugLogBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
    }
}
