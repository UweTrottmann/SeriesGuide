// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann
package com.battlelancer.seriesguide.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R

/**
 * Helper [FrameLayout] to show an empty view with a message and action button (e.g. to refresh content).
 */
class EmptyView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val emptyViewText: TextView
    private val emptyViewButton: Button

    init {
        LayoutInflater.from(context).inflate(R.layout.empty_view, this, true)
        emptyViewText = findViewById(R.id.textViewEmptyView)
        emptyViewButton = findViewById(R.id.buttonEmptyView)

        val a = context.theme
            .obtainStyledAttributes(attrs, R.styleable.EmptyView, 0, 0)

        try {
            emptyViewText.text = a.getString(R.styleable.EmptyView_emptyViewMessage)
            emptyViewButton.text = a.getString(R.styleable.EmptyView_emptyViewButtonText)
        } finally {
            a.recycle()
        }
    }

    fun setMessage(@StringRes textResId: Int) {
        emptyViewText.setText(textResId)
    }

    fun setMessage(textResId: CharSequence?) {
        emptyViewText.text = textResId
    }

    fun setButtonText(@StringRes textResId: Int) {
        emptyViewButton.setText(textResId)
    }

    fun setButtonClickListener(listener: OnClickListener?) {
        emptyViewButton.setOnClickListener(listener)
    }

    fun setContentVisibility(visibility: Int) {
        emptyViewText.visibility = visibility
        emptyViewButton.visibility = visibility
    }
}
