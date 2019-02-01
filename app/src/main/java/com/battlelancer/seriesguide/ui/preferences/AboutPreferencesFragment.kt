package com.battlelancer.seriesguide.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.Utils

/**
 * Displays information about the app, the developer and licence information about content and
 * libraries.
 */
class AboutPreferencesFragment : Fragment() {

    @BindView(R.id.textViewAboutVersion)
    internal lateinit var textVersion: TextView
    @BindView(R.id.buttonAboutWebsite)
    internal lateinit var buttonWebsite: Button
    @BindView(R.id.buttonAboutPrivacy)
    internal lateinit var buttonPrivacy: Button
    @BindView(R.id.buttonAboutTvdbTerms)
    internal lateinit var buttonTvdbTerms: Button
    @BindView(R.id.buttonAboutCreativeCommons)
    internal lateinit var buttonCreativeCommons: Button
    @BindView(R.id.buttonAboutTmdbTerms)
    internal lateinit var buttonTmdbTerms: Button
    @BindView(R.id.buttonAboutTmdbApiTerms)
    internal lateinit var buttonTmdbApiTerms: Button
    @BindView(R.id.buttonAboutTraktTerms)
    internal lateinit var buttonTraktTerms: Button
    @BindView(R.id.buttonAboutCredits)
    internal lateinit var buttonCredits: Button

    private lateinit var unbinder: Unbinder

    private val urlButtonClickListener = View.OnClickListener { onWebsiteButtonClick(it.id) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_about, container, false)
        unbinder = ButterKnife.bind(this, v)

        // display version number and database version
        textVersion.text = Utils.getVersionString(activity)

        buttonWebsite.setOnClickListener(urlButtonClickListener)
        buttonPrivacy.setOnClickListener(urlButtonClickListener)
        buttonTvdbTerms.setOnClickListener(urlButtonClickListener)
        buttonCreativeCommons.setOnClickListener(urlButtonClickListener)
        buttonTmdbTerms.setOnClickListener(urlButtonClickListener)
        buttonTmdbApiTerms.setOnClickListener(urlButtonClickListener)
        buttonTraktTerms.setOnClickListener(urlButtonClickListener)
        buttonCredits.setOnClickListener(urlButtonClickListener)

        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unbinder.unbind()
    }

    private fun onWebsiteButtonClick(@IdRes viewId: Int) {
        when (viewId) {
            R.id.buttonAboutWebsite -> viewUrl(R.string.url_website)
            R.id.buttonAboutPrivacy -> viewUrl(R.string.url_privacy)
            R.id.buttonAboutTvdbTerms -> viewUrl(R.string.url_terms_tvdb)
            R.id.buttonAboutCreativeCommons -> viewUrl(R.string.url_creative_commons)
            R.id.buttonAboutTmdbTerms -> viewUrl(R.string.url_terms_tmdb)
            R.id.buttonAboutTmdbApiTerms -> viewUrl(R.string.url_terms_tmdb_api)
            R.id.buttonAboutTraktTerms -> viewUrl(R.string.url_terms_trakt)
            R.id.buttonAboutCredits -> viewUrl(R.string.url_credits)
        }
    }

    private fun viewUrl(@StringRes urlResId: Int) {
        Utils.launchWebsite(activity, getString(urlResId))
    }
}
