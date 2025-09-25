// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.app.Activity
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ViewTools.openUriOnClick
import java.text.NumberFormat

object ViewTools {

    // Note: VectorDrawableCompat has features/fixes backported to API 21-23.
    // https://medium.com/androiddevelopers/using-vector-assets-in-android-apps-4318fd662eb9
    fun setVectorDrawableTop(textView: TextView, @DrawableRes vectorRes: Int) {
        val drawable = AppCompatResources.getDrawable(textView.context, vectorRes)
        setCompoundDrawablesWithIntrinsicBounds(textView, null, drawable)
    }

    fun setVectorDrawableLeft(textView: TextView, @DrawableRes vectorRes: Int) {
        val drawable = AppCompatResources.getDrawable(textView.context, vectorRes)
        setCompoundDrawablesWithIntrinsicBounds(textView, drawable, null)
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end of, and below the
     * text. Use null if you do not want a Drawable there. The Drawables' bounds will be set to
     * their intrinsic bounds.
     */
    private fun setCompoundDrawablesWithIntrinsicBounds(
        textView: TextView,
        left: Drawable?, top: Drawable?
    ) {
        left?.setBounds(0, 0, left.intrinsicWidth, left.intrinsicHeight)
        top?.setBounds(0, 0, top.intrinsicWidth, top.intrinsicHeight)
        textView.setCompoundDrawables(left, top, null, null)
    }

    fun setValueOrPlaceholder(view: View, value: String?) {
        val field = view as TextView
        if (value.isNullOrEmpty()) {
            field.setText(R.string.unknown)
        } else {
            field.text = value
        }
    }

    /**
     * If the given string is not null or empty, will make the label and value field [View.VISIBLE].
     * Otherwise both [View.GONE].
     *
     * @return True if the views are visible.
     */
    fun setLabelValueOrHide(label: View, text: TextView, value: String?): Boolean {
        return if (value.isNullOrEmpty()) {
            label.visibility = View.GONE
            text.visibility = View.GONE
            false
        } else {
            label.visibility = View.VISIBLE
            text.visibility = View.VISIBLE
            text.text = value
            true
        }
    }

    /**
     * If the given double is larger than 0, will make the label and value field [View.VISIBLE].
     * Otherwise both [View.GONE].
     *
     * @return True if the views are visible.
     */
    fun setLabelValueOrHide(label: View, text: TextView, value: Double?): Boolean {
        return if (value != null && value > 0.0) {
            label.visibility = View.VISIBLE
            text.visibility = View.VISIBLE
            text.text = NumberFormat.getNumberInstance().format(value)
            true
        } else {
            label.visibility = View.GONE
            text.visibility = View.GONE
            false
        }
    }

    fun setMenuItemActiveString(item: MenuItem) {
        item.title = item.title.toString() + " ◀"
    }

    /**
     * Sets a [View.OnLongClickListener] and a [View.OnContextClickListener] that enables
     * right clicks with a mouse.
     *
     * If [action] is `null`, removes the listeners.
     */
    fun View.setContextAndLongClickListener(action: (() -> Unit)?) {
        if (action == null) {
            setOnLongClickListener(null)
            setOnContextClickListener(null)
        } else {
            setOnLongClickListener {
                action()
                true
            }
            setOnContextClickListener {
                action()
                true
            }
        }
    }

    @JvmStatic
    fun setSwipeRefreshLayoutColors(
        theme: Resources.Theme,
        swipeRefreshLayout: SwipeRefreshLayout
    ) {
        val accentColorResId =
            ThemeUtils.resolveAttributeToResourceId(theme, androidx.appcompat.R.attr.colorAccent)
        swipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.sg_color_secondary)
    }

    fun showSoftKeyboardOnSearchView(window: Window, searchView: View) {
        // https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility#ShowReliably
        searchView.requestFocus()
        WindowCompat.getInsetsController(window, searchView).show(WindowInsetsCompat.Type.ime())
    }

    /**
     * If there is a [Activity.getCurrentFocus] will hide the soft input method, if available.
     */
    fun Activity.hideSoftKeyboard() {
        currentFocus?.let { view ->
            val imm = getSystemService<InputMethodManager>()
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Sets a [View.OnClickListener] that opens the [uri] using [WebTools.openInApp].
     */
    fun View.openUriOnClick(uri: String?) {
        setOnClickListener { v: View ->
            if (uri != null) WebTools.openInApp(v.context, uri)
        }
    }

    /**
     * Like [openUriOnClick], but also calls [copyTextToClipboardOnLongClick] with the [uri].
     */
    fun View.openUrlOnClickAndCopyOnLongPress(uri: String) {
        openUriOnClick(uri)
        copyTextToClipboardOnLongClick(uri)
    }

    fun configureNotMigratedWarning(view: View, notMigrated: Boolean) {
        view.visibility = if (notMigrated) View.VISIBLE else View.GONE
        if (notMigrated) {
            view.openUriOnClick(view.context.getString(R.string.url_tmdb_migration))
        }
    }

}