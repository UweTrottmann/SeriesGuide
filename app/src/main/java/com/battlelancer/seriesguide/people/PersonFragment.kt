package com.battlelancer.seriesguide.people

import android.content.Context
import android.content.res.Configuration
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
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentPersonBinding
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.WebTools
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

    private var isShownInTwoPaneLayout = false
    private var binding: FragmentPersonBinding? = null

    private var personTmdbId: Int = 0
    private var person: Person? = null
    private val model: PersonViewModel by viewModels {
        PersonViewModelFactory(requireActivity().application, personTmdbId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        personTmdbId = arguments?.getInt(ARG_PERSON_TMDB_ID) ?: 0
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isShownInTwoPaneLayout = context is PeopleActivity
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
            // Note: When shown in two-pane layout, this is contained in a card that already
            // is adjusted for the navigation bar.
            if (!isShownInTwoPaneLayout) {
                // In single pane view in landscape adjust the person name for the navigation bar.
                val isLandscape =
                    view.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    ThemeUtils.applyBottomMarginForNavigationBar(textViewPersonName)
                }

                scrollViewPerson.apply {
                    // Get the current padding values of the view.
                    val initialPadding = ThemeUtils.InitialOffset(
                        paddingStart,
                        paddingTop,
                        paddingEnd,
                        paddingBottom
                    )

                    val navigationBarBottomPaddingListener = object :
                        ThemeUtils.OnApplyWindowInsetsInitialPaddingListener {
                        override fun onApplyWindowInsets(
                            view: View,
                            insets: WindowInsetsCompat,
                            initialOffset: ThemeUtils.InitialOffset
                        ): WindowInsetsCompat {
                            val sysBarInsets = insets.getInsets(
                                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                            )

                            val resources = view.resources
                            if (isLandscape) {
                                // In single pane view in landscape add top padding
                                // for action bar and status bar.
                                val actionBarSizePx = resources.getDimensionPixelSize(
                                    ThemeUtils.resolveAttributeToResourceId(
                                        view.context.theme,
                                        R.attr.actionBarSize
                                    )
                                )
                                initialOffset
                                    .copy(
                                        top = initialOffset.top + sysBarInsets.top + actionBarSizePx,
                                        bottom = initialOffset.bottom + sysBarInsets.bottom
                                    )
                                    .applyAsPadding(view)
                            } else {
                                initialOffset
                                    .copy(bottom = initialOffset.bottom + sysBarInsets.bottom)
                                    .applyAsPadding(view)
                            }

                            // Do *not* consume or modify insets so any other views receive them
                            // (only required for pre-R, see View.sBrokenInsetsDispatch).
                            return insets
                        }
                    }

                    // Sets an [androidx.core.view.OnApplyWindowInsetsListener] that calls the custom
                    // listener with initial padding values of this view.
                    // Note: this is based on similar code of the Material Components ViewUtils class.
                    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                        navigationBarBottomPaddingListener.onApplyWindowInsets(
                            v,
                            insets,
                            initialPadding
                        )
                    }
                }
            }

            buttonPersonTmdbLink.setOnClickListener {
                person?.id?.let {
                    WebTools.openInApp(requireContext(), TmdbTools.buildPersonUrl(it))
                }
            }
            buttonPersonTmdbLink.setOnLongClickListener {
                person?.id?.let {
                    copyTextToClipboard(view.context, TmdbTools.buildPersonUrl(it))
                    true
                }
                false
            }
            buttonPersonWebSearch.setOnClickListener {
                person?.let {
                    val name = it.name
                    if (!name.isNullOrEmpty()) {
                        ServiceUtils.performWebSearch(requireContext(), name)
                    }
                }
            }
        }

        setProgressVisibility(true)
        model.personLiveData.observe(viewLifecycleOwner) {
            setProgressVisibility(false)
            populatePersonViews(it)
        }
        model.languageCode.value = PeopleSettings.getPersonLanguage(requireContext())

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.person_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_person_language -> {
                    L10nDialogFragment.show(
                        parentFragmentManager,
                        model.languageCode.value,
                        "person-language-dialog"
                    )
                    true
                }
                else -> false
            }
        }
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
        PeopleSettings.setPersonLanguage(requireContext(), event.selectedLanguageCode)
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
                    model.languageCode.value,
                    PeopleSettings.getPersonLanguage(requireContext())
                )
            } else {
                person.biography
            }
            textViewPersonBiography.text = TextTools.textWithTmdbSource(
                textViewPersonBiography.context,
                biography
            )

            if (!TextUtils.isEmpty(person.profile_path)) {
                ImageTools.loadWithPicasso(
                    requireContext(),
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
