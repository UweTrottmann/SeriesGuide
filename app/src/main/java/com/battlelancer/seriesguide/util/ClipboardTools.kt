@file:JvmName("ClipboardTools")

package com.battlelancer.seriesguide.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.battlelancer.seriesguide.R

fun copyTextToClipboard(context: Context, text: CharSequence): Boolean {
    val clip = ClipData.newPlainText("text", text)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    return if (clipboard != null) {
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.copy_to_clipboard, Toast.LENGTH_SHORT).show()
        true
    } else {
        false
    }
}

private val onLongClickListener = View.OnLongClickListener {
    return@OnLongClickListener it is TextView && copyTextToClipboard(it.context, it.text)
}

fun TextView.copyTextToClipboardOnLongClick() {
    // globally shared click listener instance
    setOnLongClickListener(onLongClickListener)
}

fun View.copyTextToClipboardOnLongClick(text: CharSequence) {
    setOnLongClickListener { v -> copyTextToClipboard(v.context, text) }
}

private val onClickListener = View.OnClickListener {
    if (it is TextView) {
        copyTextToClipboard(it.context, it.text)
    }
}

fun TextView.copyTextToClipboardOnClick() {
    // globally shared click listener instance
    setOnClickListener(onClickListener)
}
