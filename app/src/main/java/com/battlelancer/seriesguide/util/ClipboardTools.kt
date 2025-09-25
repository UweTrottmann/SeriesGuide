// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

@file:JvmName("ClipboardTools")

package com.battlelancer.seriesguide.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.battlelancer.seriesguide.R

fun copyTextToClipboard(context: Context, text: CharSequence): Boolean {
    // Label name taken from https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#kotlin
    val clip = ClipData.newPlainText("simple text", text)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    return if (clipboard != null) {
        clipboard.setPrimaryClip(clip)
        // On Android 13+ the system shows a copied to clipboard notification or popup
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(context, R.string.copy_to_clipboard, Toast.LENGTH_SHORT).show()
        }
        true
    } else {
        false
    }
}

private val onLongClickListener = View.OnLongClickListener {
    return@OnLongClickListener it is TextView && copyTextToClipboard(it.context, it.text)
}

// Use lazy to avoid crashing on older Android versions where OnContextClickListener is not available
private val onContextClickListener by lazy {
    View.OnContextClickListener {
        return@OnContextClickListener it is TextView && copyTextToClipboard(it.context, it.text)
    }
}

/**
 * Sets a [View.OnLongClickListener] and a [View.OnContextClickListener] that enables right clicks
 * with a mouse.
 */
fun TextView.copyTextToClipboardOnLongClick() {
    // globally shared click listener instances
    setOnLongClickListener(onLongClickListener)
    setOnContextClickListener(onContextClickListener)
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
