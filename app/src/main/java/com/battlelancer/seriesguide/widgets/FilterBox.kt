package com.battlelancer.seriesguide.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ViewTools

/**
 * Three state check box for filter being disabled, included or excluded.
 */
class FilterBox @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val filterDescription: String

    @BindView(R.id.textViewFilterBox)
    lateinit var textView: TextView
    @BindView(R.id.imageViewFilterBox)
    lateinit var imageView: ImageView

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_filter_box, this)

        val a = context.theme
            .obtainStyledAttributes(attrs, R.styleable.FilterBox, 0, 0)
        try {
            filterDescription = a.getString(R.styleable.FilterBox_filterBoxDescription) ?: ""
        } finally {
            a.recycle()
        }

        ButterKnife.bind(this)
        textView.text = filterDescription
    }

    /**
     * Pass `null` to disable filter, `true` to include and `false` to exclude.
     */
    var state: Boolean? = null
        set(value) {
            field = value
            updateImage()
        }

    private fun updateImage() {
        val vectorDrawable = when (state) {
            null -> ViewTools.vectorIconActive(context, context.theme, R.drawable.ic_box_blank_white_24dp)
            true -> VectorDrawableCompat.create(context.resources, R.drawable.ic_box_plus_green_24dp, context.theme)
            false -> VectorDrawableCompat.create(context.resources, R.drawable.ic_box_minus_red_24dp, context.theme)
        }
        imageView.setImageDrawable(vectorDrawable)
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