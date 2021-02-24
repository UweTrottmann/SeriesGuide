package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Setter
import butterknife.Unbinder
import butterknife.ViewCollections
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.dialogs.ShowL10nDialogFragment
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.copyTextToClipboard
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.util.safeShow
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.CheatSheet
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

    @BindView(R.id.containerShowInfo)
    lateinit var containerShowInfo: ViewGroup
    @BindView(R.id.textViewAddTitle)
    lateinit var title: TextView
    @BindView(R.id.textViewAddShowMeta)
    lateinit var showmeta: TextView
    @BindView(R.id.buttonAddLanguage)
    lateinit var buttonLanguage: Button
    @BindView(R.id.buttonAddDisplaySimilar)
    lateinit var buttonDisplaySimilar: Button
    @BindView(R.id.buttonAddStreamingSearch)
    lateinit var buttonStreamingSearch: Button
    @BindView(R.id.textViewAddDescription)
    lateinit var overview: TextView
    @BindView(R.id.textViewAddRatingValue)
    lateinit var rating: TextView
    @BindView(R.id.textViewAddRatingRange)
    lateinit var ratingRange: TextView
    @BindView(R.id.textViewAddGenres)
    lateinit var genres: TextView
    @BindView(R.id.textViewAddReleased)
    lateinit var releasedTextView: TextView
    @BindView(R.id.imageViewAddPoster)
    lateinit var poster: ImageView

    @BindViews(
        R.id.textViewAddRatingValue,
        R.id.textViewAddRatingLabel,
        R.id.textViewAddRatingRange,
        R.id.textViewAddGenresLabel
    )
    lateinit var labelViews: List<@JvmSuppressWildcards View>

    @BindView(R.id.buttonPositive)
    lateinit var buttonPositive: Button
    @BindView(R.id.buttonNegative)
    lateinit var buttonNegative: Button
    @BindView(R.id.progressBarAdd)
    lateinit var progressBar: View

    private lateinit var unbinder: Unbinder
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_addshow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        unbinder = ButterKnife.bind(this, view)

        // Long press hint.
        CheatSheet.setup(buttonLanguage, R.string.pref_language)
        // Buttons.
        buttonStreamingSearch.isGone = StreamingSearch.isNotConfiguredOrTurnedOff(requireContext())
        buttonNegative.setText(R.string.dismiss)
        buttonNegative.setOnClickListener { dismiss() }
        buttonPositive.isGone = true

        ratingRange.text = getString(R.string.format_rating_range, 10)

        // Set up long-press to copy text to clipboard (d-pad friendly vs text selection).
        containerShowInfo.setOnLongClickListener {
            // Just copy text from views instead of re-building.
            val summaryBuilder = StringBuilder().apply {
                append(title.text).append("\n")
                append(releasedTextView.text).append("\n")
                append(showmeta.text)
            }
            copyTextToClipboard(it.context, summaryBuilder)
        }
        overview.copyTextToClipboardOnLongClick()
        genres.copyTextToClipboardOnLongClick()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (showTmdbId <= 0) {
            Timber.e("Not a valid show, closing.")
            dismiss()
            // Note: After dismiss still continues through lifecycle methods.
            return
        }

        // Load show details.
        showProgressBar(true)
        model.showDetails.observe(viewLifecycleOwner) { show ->
            showProgressBar(false)
            overview.isGone = false
            populateShowViews(show)
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
        unbinder.unbind()
    }

    @OnClick(R.id.buttonAddLanguage)
    fun onClickButtonLanguage() {
        ShowL10nDialogFragment.show(
            parentFragmentManager,
            languageCode,
            ShowL10nDialogFragment.TAG_ADD_DIALOG
        )
    }

    @OnClick(R.id.buttonAddDisplaySimilar)
    fun onClickButtonDisplaySimilarShows() {
        val details = model.showDetails.value
        if (details?.show != null) {
            dismissAllowingStateLoss()
            SimilarShowsFragment.displaySimilarShowsEventLiveData.postValue(SearchResult().also {
                it.tmdbId = showTmdbId
                it.title = details.show.title
            })
        }
    }

    @OnClick(R.id.buttonAddStreamingSearch)
    fun onClickButtonStreamingSearch() {
        model.showDetails.value?.show?.title?.let {
            StreamingSearch.searchForShow(requireContext(), it)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ShowL10nDialogFragment.LanguageChangedEvent) {
        if (ShowL10nDialogFragment.TAG_ADD_DIALOG != event.tag) {
            return
        }

        // Reload show details with new language.
        showProgressBar(true)
        overview.visibility = View.INVISIBLE

        this.languageCode = event.selectedLanguageCode
        model.languageCode.value = event.selectedLanguageCode
    }

    private fun populateShowViews(result: AddShowDialogViewModel.ShowDetails) {
        val show = result.show
        if (show == null) {
            // Failed to load, can't be added.
            if (!AndroidUtils.isNetworkConnected(requireContext())) {
                overview.setText(R.string.offline)
            } else if (result.doesNotExist) {
                overview.setText(R.string.tvdb_error_does_not_exist)
            } else {
                overview.text = getString(
                    R.string.api_error_generic,
                    "${getString(R.string.tmdb)}/${getString(R.string.trakt)}"
                )
            }
            return
        }
        if (result.localShowId != null) {
            // Already added, offer to open show instead.
            buttonPositive.setText(R.string.action_open)
            buttonPositive.setOnClickListener {
                startActivity(OverviewActivity.intentShow(context, result.localShowId))
                dismiss()
            }
        } else {
            // Not added, offer to add.
            buttonPositive.setText(R.string.action_shows_add)
            buttonPositive.setOnClickListener {
                EventBus.getDefault().post(AddFragment.OnAddingShowEvent(showTmdbId))
                addShowListener.onAddShow(SearchResult().also {
                    it.tmdbId = showTmdbId
                    it.title = show.title
                    it.language = languageCode
                })
                dismiss()
            }
        }
        buttonPositive.isGone = false

        buttonLanguage.text = LanguageTools.getShowLanguageStringFor(context, languageCode)

        // Title, overview.
        title.text = show.title
        overview.text = show.overview

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
                val style = if (status == ShowTools.Status.CONTINUING) {
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
        releasedTextView.text = statusText

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
        showmeta.text = timeAndNetworkText

        // Rating.
        rating.text = TraktTools.buildRatingString(show.ratingGlobal)

        // Genres.
        ViewTools.setValueOrPlaceholder(genres, TextTools.splitAndKitTVDBStrings(show.genres))

        // Poster.
        ImageTools.loadShowPosterFitCrop(show.posterSmall, poster, requireActivity())

        // Enable adding of show, display views.
        buttonPositive.isEnabled = true
        ViewCollections.set(labelViews, VISIBLE_SETTER, true)
    }

    private fun showProgressBar(isVisible: Boolean) {
        progressBar.isGone = !isVisible
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
        fun show(context: Context, fm: FragmentManager, showTmdbId: Int) {
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

        val VISIBLE_SETTER = Setter<View, Boolean> { view, value, _ ->
            view.visibility = if (value == true) View.VISIBLE else View.INVISIBLE
        }
    }
}
