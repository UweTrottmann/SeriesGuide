package com.battlelancer.seriesguide.ui.people

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentPersonBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TmdbTools
import com.battlelancer.seriesguide.util.copyTextToClipboard
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.Person
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays details about a crew or cast member and their work.
 */
class PersonFragment : Fragment() {

    private var binding: FragmentPersonBinding? = null

    private var personTmdbId: Int = 0
    private var person: Person? = null
    private val model: PersonViewModel by viewModels {
        PersonViewModelFactory(requireActivity().application, personTmdbId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        personTmdbId = arguments?.getInt(ARG_PERSON_TMDB_ID) ?: 0
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPersonBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            buttonPersonTmdbLink.setOnClickListener {
                person?.id?.let { TmdbTools.openTmdbPerson(activity, it) }
            }
            buttonPersonTmdbLink.setOnLongClickListener {
                person?.id?.let {
                    copyTextToClipboard(view.context, TmdbTools.buildPersonUrl(it))
                    true
                }
                false
            }
            buttonPersonWebSearch.setOnClickListener {
                person?.let { ServiceUtils.performWebSearch(activity, it.name) }
            }
        }

        setProgressVisibility(true)
        model.personLiveData.observe(viewLifecycleOwner, {
            setProgressVisibility(false)
            populatePersonViews(it)
        })
        model.languageCode.value = DisplaySettings.getPersonLanguage(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.person_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_action_person_language) {
            L10nDialogFragment.forPerson(
                parentFragmentManager,
                model.languageCode.value,
                "person-language-dialog"
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: L10nDialogFragment.LanguageChangedEvent) {
        DisplaySettings.setPersonLanguage(requireContext(), event.selectedLanguageCode)
        model.languageCode.value = event.selectedLanguageCode
    }

    private fun populatePersonViews(person: Person?) {
        binding?.apply {
            if (person == null) {
                if (AndroidUtils.isNetworkConnected(requireContext())) {
                    textViewPersonBiography.text = getString(
                        R.string.api_error_generic,
                        getString(R.string.tmdb)
                    )
                } else {
                    Toast.makeText(activity, R.string.offline, Toast.LENGTH_SHORT).show()
                    textViewPersonBiography.text = getString(R.string.offline)
                }
                return
            }

            this@PersonFragment.person = person

            textViewPersonName.text = person.name
            val biography = if (TextUtils.isEmpty(person.biography)) {
                TextTools.textNoTranslationMovieLanguage(
                    requireContext(),
                    model.languageCode.value
                )
            } else {
                person.biography
            }
            textViewPersonBiography.text = TextTools.textWithTmdbSource(
                textViewPersonBiography.context,
                biography
            )

            if (!TextUtils.isEmpty(person.profile_path)) {
                ServiceUtils.loadWithPicasso(
                    activity,
                    TmdbTools.buildProfileImageUrl(
                        activity, person.profile_path, TmdbTools.ProfileImageSize.H632
                    )
                ).placeholder(
                    ColorDrawable(
                        ContextCompat.getColor(requireContext(), R.color.protection_dark)
                    )
                ).into(imageViewPersonHeadshot)
            }

            // show actions
            requireActivity().invalidateOptionsMenu()
        }
    }

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    private fun setProgressVisibility(isVisible: Boolean) {
        binding?.apply {
            if (progressBarPerson.visibility == (if (isVisible) View.VISIBLE else View.GONE)) {
                // already in desired state, avoid replaying animation
                return
            }
            progressBarPerson.startAnimation(
                AnimationUtils.loadAnimation(
                    progressBarPerson.context,
                    if (isVisible) R.anim.fade_in else R.anim.fade_out
                )
            )
            progressBarPerson.visibility = if (isVisible) View.VISIBLE else View.GONE
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
