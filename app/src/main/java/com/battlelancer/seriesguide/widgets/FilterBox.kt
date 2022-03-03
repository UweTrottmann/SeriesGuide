package com.battlelancer.seriesguide.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewFilterBoxBinding
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * Three state check box for filter being disabled, included or excluded.
 */
class FilterBox @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val filterDescription: String

    private val binding = ViewFilterBoxBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL

        val a = context.theme
            .obtainStyledAttributes(attrs, R.styleable.FilterBox, 0, 0)
        try {
            filterDescription = a.getString(R.styleable.FilterBox_filterBoxDescription) ?: ""
        } finally {
            a.recycle()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        updateState()
    }

    /**
     * Pass `null` to disable filter, `true` to include and `false` to exclude.
     */
    var state: Boolean? = null
        set(value) {
            field = value
            updateState()
        }

    private fun updateState() {
        val vectorDrawable = when (state) {
            null -> VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_box_blank_white_24dp,
                context.theme
            )
            true -> VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_box_plus_green_24dp,
                context.theme
            )
            false -> VectorDrawableCompat.create(
                context.resources,
                R.drawable.ic_box_minus_red_24dp,
                context.theme
            )
        }
        val stateDescription = when (state) {
            null -> context.getString(R.string.state_shows_filter_disabled)
            true -> context.getString(R.string.state_shows_filter_included)
            false -> context.getString(R.string.state_shows_filter_excluded)
        }
        binding.imageViewFilterBox.setImageDrawable(vectorDrawable)
        if (AndroidUtils.isAtLeastR) {
            binding.textViewFilterBox.stateDescription = stateDescription
        } else {
            // Android 11+ (above) read state, then description.
            binding.textViewFilterBox.contentDescription =
                if (binding.textViewFilterBox.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    "$filterDescription ,$stateDescription"
                } else {
                    "$stateDescription, $filterDescription"
                }
        }
        binding.textViewFilterBox.text =
            TextTools.buildTitleAndSummary(context, filterDescription, stateDescription)
    }

    override fun performClick(): Boolean {
        moveToNextState()
        return super.performClick()
    }

    private fun moveToNextState() {
        state = when (state) {
            null -> true
            true -> false
            false -> null
        }
    }

}