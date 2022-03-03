package com.battlelancer.seriesguide.ui.search

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.TooltipCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.databinding.DialogAddshowBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboard
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uwetrottmann.androidutils.AndroidUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * A [DialogFragment] allowing the user to decide whether to add a show to SeriesGuide.
 * Displays show details as well.
 */
class AddShowDialogFragment : AppCompatDialogFragment() {

    interface OnAddShowListener {
        fun onAddShow(show: SearchResult)
    }

    private var binding: DialogAddshowBinding? = null
    private lateinit var addShowListener: OnAddShowListener
    private var showTmdbId: Int = 0
    private lateinit var languageCode: String
    private val model by viewModels<AddShowDialogViewModel> {
        AddShowDialogViewModelFactory(requireActivity().application, showTmdbId, languageCode)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            addShowListener = context as OnAddShowListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnAddShowListener")
        }

        showTmdbId = requireArguments().getInt(ARG_INT_SHOW_TMDBID)
        val languageCodeOrNull = requireArguments().getString(ARG_STRING_LANGUAGE_CODE)
        if (languageCodeOrNull.isNullOrEmpty()) {
            // Use search language.
            this.languageCode = DisplaySettings.getShowsSearchLanguage(context)
        } else {
            this.languageCode = languageCodeOrNull
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide title, use custom theme
        setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    }

    // Note: using onCreateDialog as onCreateView does not have proper auto-sizing depending on screen.
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddshowBinding.inflate(layoutInflater).also {
            this.binding = it
        }
        binding.apply {
            // Long press hint.
            TooltipCompat.setTooltipText(
                buttonAddLanguage,
                buttonAddLanguage.context.getString(R.string.pref_language)
            )
            // Buttons.
            StreamingSearch.initButtons(
                buttonAddStreamingSearch, buttonAddStreamingSearchInfo, parentFragmentManager
            )
            textViewAddStreamingSearch.isGone = true
            buttonNegative.apply {
                setText(R.string.dismiss)
                setOnClickListener { dismiss() }
            }
            buttonPositive.isGone = true

            textViewAddRatingRange.text = getString(R.string.format_rating_range, 10)

            // Set up long-press to copy text to clipboard (d-pad friendly vs text selection).
            containerShowInfo.setOnLongClickListener {
                // Just copy text from views instead of re-building.
                val summaryBuilder = StringBuilder().apply {
                    append(textViewAddTitle.text).append("\n")
                    append(textViewAddReleased.text).append("\n")
                    append(textViewAddShowMeta.text)
                }
                copyTextToClipboard(it.context, summaryBuilder)
            }
            textViewAddDescription.copyTextToClipboardOnLongClick()
            textViewAddGenres.copyTextToClipboardOnLongClick()

            buttonAddLanguage.setOnClickListener {
                L10nDialogFragment.forShow(
                    parentFragmentManager,
                    languageCode,
                    L10nDialogFragment.TAG_ADD_DIALOG
                )
            }

            buttonAddDisplaySimilar.setOnClickListener {
                val details = model.showDetails.value
                if (details?.show != null) {
                    dismissAllowingStateLoss()
                    SimilarShowsFragment.displaySimilarShowsEventLiveData.postValue(SearchResult().also {
                        it.tmdbId = showTmdbId
                        it.title = details.show.title
                    })
                }
            }
        }

        if (showTmdbId <= 0) {
            Timber.e("Not a valid show, closing.")
            dismiss()
            // Note: After dismiss still continues through lifecycle methods.
        } else {
            // Load show details.
            showProgressBar(true)
            // Note: viewLifeCycleOwner not available for DialogFragment, use DialogFragment itself.
            model.showDetails.observe(this) { show ->
                showProgressBar(false)
                this.binding?.textViewAddDescription?.isGone = false
                populateShowViews(show)
            }
            model.watchProvider.observe(this, { watchInfo ->
                this.binding?.buttonAddStreamingSearch?.let {
                    val providerInfo = StreamingSearch.configureButton(it, watchInfo, false)
                    this.binding?.textViewAddStreamingSearch?.apply {
                        text = providerInfo
                        isGone = providerInfo == null
                    }
                }
            })
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
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
        if (L10nDialogFragment.TAG_ADD_DIALOG != event.tag) {
            return
        }

        // Reload show details with new language.
        showProgressBar(true)
        binding?.textViewAddDescription?.visibility = View.INVISIBLE

        this.languageCode = event.selectedLanguageCode
        model.languageCode.value = event.selectedLanguageCode
    }

    private fun populateShowViews(result: AddShowDialogViewModel.ShowDetails) {
        val binding = this.binding!!

        val show = result.show
        if (show == null) {
            // Failed to load, can't be added.
            if (!AndroidUtils.isNetworkConnected(requireContext())) {
                binding.textViewAddDescription.setText(R.string.offline)
            } else if (result.doesNotExist) {
                binding.textViewAddDescription.setText(R.string.tvdb_error_does_not_exist)
            } else {
                binding.textViewAddDescription.text = getString(
                    R.string.api_error_generic,
                    "${getString(R.string.tmdb)}/${getString(R.string.trakt)}"
                )
            }
            return
        }

        if (result.localShowId != null) {
            // Already added, offer to open show instead.
            binding.buttonPositive.setText(R.string.action_open)
            binding.buttonPositive.setOnClickListener {
                startActivity(OverviewActivity.intentShow(requireContext(), result.localShowId))
                dismiss()
            }
        } else {
            // Not added, offer to add.
            binding.buttonPositive.setText(R.string.action_shows_add)
            binding.buttonPositive.setOnClickListener {
                EventBus.getDefault().post(AddFragment.OnAddingShowEvent(showTmdbId))
                addShowListener.onAddShow(SearchResult().also {
                    it.tmdbId = showTmdbId
                    it.title = show.title
                    it.language = languageCode
                })
                dismiss()
            }
        }
        binding.buttonPositive.isGone = false

        binding.buttonAddLanguage.text =
            LanguageTools.getShowLanguageStringFor(requireContext(), languageCode)

        // Title, overview.
        binding.textViewAddTitle.text = show.title
        binding.textViewAddDescription.text = show.overview

        // Release year.
        val statusText = SpannableStringBuilder().also { statusText ->
            TimeTools.getShowReleaseYear(show.firstRelease)?.let {
                statusText.append(it)
            }
            // Continuing/ended status.
            val status = show.statusOrUnknown
            val statusString = SgApp.getServicesComponent(requireContext()).showTools()
                .getStatus(status)
            if (statusString != null) {
                if (statusText.isNotEmpty()) {
                    statusText.append(" / ") // Like "2016 / Continuing".
                }

                val currentTextLength = statusText.length
                statusText.append(statusString)

                // If continuing, paint status green.
                val style = if (status == ShowTools.Status.RETURNING) {
                    R.style.TextAppearance_SeriesGuide_Body2_Accent
                } else {
                    R.style.TextAppearance_SeriesGuide_Body2_Secondary
                }
                statusText.setSpan(
                    TextAppearanceSpan(activity, style),
                    currentTextLength,
                    statusText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        binding.textViewAddReleased.text = statusText

        // Next release day and time.
        val timeAndNetworkText = SpannableStringBuilder().apply {
            if (show.releaseTimeOrDefault != -1) {
                val release = TimeTools.getShowReleaseDateTime(
                    requireContext(),
                    show.releaseTimeOrDefault,
                    show.releaseWeekDayOrDefault,
                    show.releaseTimeZone,
                    show.releaseCountry,
                    show.network
                )
                val day = TimeTools.formatToLocalDayOrDaily(
                    requireContext(),
                    release,
                    show.releaseWeekDayOrDefault
                )
                val time = TimeTools.formatToLocalTime(requireContext(), release)
                append(day).append(" ").append(time)
                append("\n")
            }

            // Network, runtime.
            append(show.network)
            append("\n")
            append(getString(R.string.runtime_minutes, show.runtime.toString()))
        }
        binding.textViewAddShowMeta.text = timeAndNetworkText

        // Rating.
        binding.textViewAddRatingValue.text = TraktTools.buildRatingString(show.ratingGlobal)

        // Genres.
        ViewTools.setValueOrPlaceholder(
            binding.textViewAddGenres,
            TextTools.splitPipeSeparatedStrings(show.genres)
        )

        // Poster.
        ImageTools.loadShowPosterFitCrop(
            show.posterSmall,
            binding.imageViewAddPoster,
            requireActivity()
        )

        // Enable adding of show.
        binding.buttonPositive.isEnabled = true
    }

    private fun showProgressBar(isVisible: Boolean) {
        binding?.progressBarAdd?.isGone = !isVisible
    }

    companion object {

        private const val TAG = "AddShowDialogFragment"
        private const val ARG_INT_SHOW_TMDBID = "show_tmdbid"
        private const val ARG_STRING_LANGUAGE_CODE = "language"

        /**
         * Display a [AddShowDialogFragment] for the given show. The language of the show should
         * be set.
         */
        @JvmStatic
        fun show(fm: FragmentManager, show: SearchResult) {
            // Replace any currently showing add dialog (do not add it to the back stack).
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            newInstance(show.tmdbId, show.language).safeShow(fm, ft, TAG)
        }

        /**
         * Display a [AddShowDialogFragment] for the given show.
         *
         * Use if there is no actual search result, but just a TheTVDB id available. Uses the search
         * or fall back language.
         */
        @JvmStatic
        fun show(fm: FragmentManager, showTmdbId: Int) {
            val fakeResult = SearchResult().apply {
                tmdbId = showTmdbId
            }
            show(fm, fakeResult)
        }

        private fun newInstance(showTmdbId: Int, languageCode: String?): AddShowDialogFragment {
            return AddShowDialogFragment().apply {
                arguments = bundleOf(
                    ARG_INT_SHOW_TMDBID to showTmdbId,
                    ARG_STRING_LANGUAGE_CODE to languageCode
                )
            }
        }

    }
}
