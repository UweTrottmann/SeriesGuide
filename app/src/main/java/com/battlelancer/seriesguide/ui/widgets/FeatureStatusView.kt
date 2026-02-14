// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewFeatureStatusBinding

/**
 * A visual indicator and description of a feature that can have one of [FeatureState].
 */
class FeatureStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    enum class FeatureState(val value: Int) {
        // The values must match the featureState enum declared in attrs.xml
        NOT_SUPPORTED(0),
        SUPPORTED(1),
        DISABLED(2)
    }

    private val featureState: Int
    private val featureDescription: String?
    private val binding: ViewFeatureStatusBinding

    init {
        val a = context.theme
            .obtainStyledAttributes(attrs, R.styleable.FeatureStatusView, 0, 0)
        try {
            featureState = a.getInt(
                R.styleable.FeatureStatusView_featureState,
                FeatureState.SUPPORTED.value
            )
            featureDescription = a.getString(R.styleable.FeatureStatusView_featureDescription)
        } finally {
            a.recycle()
        }

        orientation = HORIZONTAL
        binding = ViewFeatureStatusBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateFeatureState(featureState)
        binding.textViewStatus.text = featureDescription
    }

    private fun updateFeatureState(state: Int) {
        val iconRes: Int
        val contentDescriptionRes: Int

        when (state) {
            FeatureState.DISABLED.value -> {
                iconRes = R.drawable.ic_cancel_red_24dp
                contentDescriptionRes = R.string.feature_disabled
            }

            FeatureState.NOT_SUPPORTED.value -> {
                iconRes = R.drawable.ic_remove_circle_black_24dp
                contentDescriptionRes = R.string.feature_not_supported
            }

            else -> {
                iconRes = R.drawable.ic_check_circle_green_24dp
                contentDescriptionRes = R.string.feature_supported
            }
        }

        val drawable = VectorDrawableCompat.create(context.resources, iconRes, context.theme)
        binding.imageViewStatus.setImageDrawable(drawable)
        binding.imageViewStatus.contentDescription = context.getString(contentDescriptionRes)
    }

    fun setFeatureState(state: FeatureState) {
        updateFeatureState(state.value)
    }
}
