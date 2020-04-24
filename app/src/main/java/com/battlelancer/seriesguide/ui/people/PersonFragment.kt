package com.battlelancer.seriesguide.ui.people

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TmdbTools
import com.battlelancer.seriesguide.util.copyTextToClipboard
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.Person

/**
 * Displays details about a crew or cast member and their work.
 */
class PersonFragment : Fragment() {

    @BindView(R.id.progressBarPerson)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.imageViewPersonHeadshot)
    lateinit var imageViewHeadshot: ImageView
    @BindView(R.id.textViewPersonName)
    lateinit var textViewName: TextView
    @BindView(R.id.textViewPersonBiography)
    lateinit var textViewBiography: TextView
    @BindView(R.id.buttonPersonTmdbLink)
    lateinit var buttonTmdbLink: Button
    @BindView(R.id.buttonPersonWebSearch)
    lateinit var buttonWebSearch: Button
    private lateinit var unbinder: Unbinder

    private var personTmdbId: Int = 0
    private var person: Person? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        personTmdbId = arguments?.getInt(ARG_PERSON_TMDB_ID) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_person, container, false)
        unbinder = ButterKnife.bind(this, rootView)
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        LoaderManager.getInstance(this)
            .initLoader(PeopleActivity.PERSON_LOADER_ID, null, personLoaderCallbacks)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unbinder.unbind()
    }

    @OnClick(R.id.buttonPersonTmdbLink)
    fun onClickButtonTmdbLink() {
        person?.id?.let { TmdbTools.openTmdbPerson(activity, it) }
    }

    @OnLongClick(R.id.buttonPersonTmdbLink)
    fun onLongClickButtonTmdbLink(view: View): Boolean {
        person?.id?.let {
            copyTextToClipboard(view.context, TmdbTools.buildPersonUrl(it))
            return true
        }
        return false
    }

    @OnClick(R.id.buttonPersonWebSearch)
    fun onClickButtonWebSearch() {
        person?.let { ServiceUtils.performWebSearch(activity, it.name) }
    }

    private fun populatePersonViews(person: Person?) {
        if (person == null) {
            if (AndroidUtils.isNetworkConnected(requireContext())) {
                textViewBiography.text = getString(R.string.api_error_generic,
                        getString(R.string.tmdb))
            } else {
                Toast.makeText(activity, R.string.offline, Toast.LENGTH_SHORT).show()
                textViewBiography.text = getString(R.string.offline)
            }
            return
        }

        this.person = person

        textViewName.text = person.name
        val biography = if (TextUtils.isEmpty(person.biography)) {
            getString(R.string.not_available)
        } else {
            person.biography
        }
        textViewBiography.text = TextTools.textWithTmdbSource(textViewBiography.context,
                biography)

        if (!TextUtils.isEmpty(person.profile_path)) {
            ServiceUtils.loadWithPicasso(activity,
                    TmdbTools.buildProfileImageUrl(activity, person.profile_path,
                            TmdbTools.ProfileImageSize.H632))
                    .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(),
                            R.color.protection_dark)))
                    .into(imageViewHeadshot)
        }

        // show actions
        requireActivity().invalidateOptionsMenu()
    }

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    private fun setProgressVisibility(isVisible: Boolean) {
        if (progressBar.visibility == (if (isVisible) View.VISIBLE else View.GONE)) {
            // already in desired state, avoid replaying animation
            return
        }
        progressBar.startAnimation(AnimationUtils.loadAnimation(progressBar.context,
                if (isVisible) R.anim.fade_in else R.anim.fade_out))
        progressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private val personLoaderCallbacks = object : LoaderManager.LoaderCallbacks<Person?> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Person?> {
            setProgressVisibility(true)
            return PersonLoader(requireContext(), personTmdbId)
        }

        override fun onLoadFinished(loader: Loader<Person?>, data: Person?) {
            if (!isAdded) {
                return
            }
            setProgressVisibility(false)
            populatePersonViews(data)
        }

        override fun onLoaderReset(loader: Loader<Person?>) {
            // do nothing, preferring stale data over no data
        }
    }

    companion object {

        const val ARG_PERSON_TMDB_ID = "person_tmdb_id"

        @JvmStatic
        fun newInstance(tmdbId: Int): PersonFragment {
            val f = PersonFragment()

            val args = Bundle().apply {
                putInt(ARG_PERSON_TMDB_ID, tmdbId)
            }
            f.arguments = args

            return f
        }
    }
}
