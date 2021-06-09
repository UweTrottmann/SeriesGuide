package com.battlelancer.seriesguide.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentAboutBinding
import com.battlelancer.seriesguide.util.Utils

/**
 * Displays information about the app, the developer and licence information about content and
 * libraries.
 */
class AboutPreferencesFragment : Fragment() {

    private var binding: FragmentAboutBinding? = null
    private val urlButtonClickListener = View.OnClickListener { onWebsiteButtonClick(it.id) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentAboutBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.apply {
            // display version number and database version
            textViewAboutVersion.text = Utils.getVersionString(activity)

            buttonAboutWebsite.setOnClickListener(urlButtonClickListener)
            buttonAboutPrivacy.setOnClickListener(urlButtonClickListener)
            buttonAboutTmdbTerms.setOnClickListener(urlButtonClickListener)
            buttonAboutTmdbApiTerms.setOnClickListener(urlButtonClickListener)
            buttonAboutTraktTerms.setOnClickListener(urlButtonClickListener)
            buttonAboutCredits.setOnClickListener(urlButtonClickListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun onWebsiteButtonClick(@IdRes viewId: Int) {
        when (viewId) {
            R.id.buttonAboutWebsite -> viewUrl(R.string.url_website)
            R.id.buttonAboutPrivacy -> viewUrl(R.string.url_privacy)
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
