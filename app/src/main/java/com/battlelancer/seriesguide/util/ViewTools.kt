package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            text.text = value.toString()
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

    @JvmStatic
    fun setSwipeRefreshLayoutColors(
        theme: Resources.Theme?,
        swipeRefreshLayout: SwipeRefreshLayout
    ) {
        val accentColorResId = Utils.resolveAttributeToResourceId(theme, R.attr.colorAccent)
        swipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.sg_color_secondary)
    }

    fun showSoftKeyboardOnSearchView(context: Context, searchView: View) {
        searchView.postDelayed({
            if (searchView.requestFocus()) {
                val imm = context.getSystemService<InputMethodManager>()
                imm?.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 200) // have to add a little delay (http://stackoverflow.com/a/27540921/1000543)
    }

    fun openUriOnClick(button: View?, uri: String?) {
        button?.setOnClickListener { v: View -> Utils.launchWebsite(v.context, uri) }
    }

    fun configureNotMigratedWarning(view: View, notMigrated: Boolean) {
        view.visibility = if (notMigrated) View.VISIBLE else View.GONE
        if (notMigrated) {
            view.setOnClickListener {
                Utils.launchWebsite(
                    view.context,
                    view.context.getString(R.string.url_tmdb_migration)
                )
            }
        }
    }

    /**
     * Configures button to open IMDB, if needed fetches ID from network while disabling button.
     */
    fun configureImdbButton(
        button: View,
        coroutineScope: CoroutineScope,
        context: Context,
        show: SgShow2?,
        episode: SgEpisode2
    ) {
        button.apply {
            isEnabled = true
            setOnClickListener { button ->
                // Disable button to prevent multiple presses.
                button.isEnabled = false
                coroutineScope.launch {
                    if (show?.tmdbId == null) {
                        button.isEnabled = true
                        return@launch
                    }
                    val episodeImdbId = if (!episode.imdbId.isNullOrEmpty()) {
                        episode.imdbId
                    } else {
                        withContext(Dispatchers.IO) {
                            TmdbTools2().getImdbIdForEpisode(
                                SgApp.getServicesComponent(context).tmdb().tvEpisodesService(),
                                show.tmdbId, episode.season, episode.number
                            )?.also {
                                SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                                    .updateImdbId(episode.id, it)
                            }
                        }
                    }
                    val imdbId = if (episodeImdbId.isNullOrEmpty()) {
                        show.imdbId // Fall back to show IMDb id.
                    } else {
                        episodeImdbId
                    }
                    // Leave button disabled if no id found.
                    if (!imdbId.isNullOrEmpty()) {
                        button.isEnabled = true
                        ServiceUtils.openImdb(imdbId, context)
                    }
                }
            }
        }
    }
}