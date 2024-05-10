// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.battlelancer.seriesguide.databinding.ViewRatingBinding

/**
 * A [ConstraintLayout] with [ViewRatingBinding].
 */
class RatingView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val binding: ViewRatingBinding

    init {
        binding = ViewRatingBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setRange(value: String) {
        binding.textViewRatingRange.text = value
    }

    fun setIcon(@DrawableRes icon: Int, @StringRes description: Int) {
        binding.textViewRatingIcon.apply {
            setImageResource(icon)
            contentDescription = context.getString(description)
        }
    }

    fun setValues(rating: String, votes: String) {
        binding.apply {
            textViewRatingValue.text = rating
            textViewRatingVotes.text = votes
        }
    }

}