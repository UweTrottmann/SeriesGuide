@file:JvmName("ClipboardTools")

package com.battlelancer.seriesguide.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.battlelancer.seriesguide.R

private val onLongClickListener = View.OnLongClickListener {
    if (it is TextView) {
        val clip = ClipData.newPlainText("text", it.text)
        val clipboard = it.getContext().getSystemService(
                Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null) {
            clipboard.primaryClip = clip
            Toast.makeText(it.getContext(), R.string.copy_to_clipboard,
                    Toast.LENGTH_SHORT).show()
            return@OnLongClickListener true
        }
    }
    return@OnLongClickListener false
}

fun TextView.copyTextToClipboardOnLongClick() {
    setOnLongClickListener(onLongClickListener)
}