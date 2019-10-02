package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Setter
import butterknife.Unbinder
import butterknife.ViewCollections
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment
import com.battlelancer.seriesguide.ui.shows.ShowTools
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
    private var displayedShow: SearchResult? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            addShowListener = context as OnAddShowListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnAddShowListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val searchResultArg: SearchResult? = arguments!!.getParcelable(ARG_SEARCH_RESULT)
        if (searchResultArg == null || searchResultArg.tvdbid <= 0) {
            // Not a valid TVDb id or show.
            displayedShow = null
            Timber.e("Not a valid show, closing.")
            dismiss()
            // Note: After dismiss still continues through lifecycle methods.
        } else {
            displayedShow = searchResultArg
        }

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

        // Icons.
        ViewTools.setVectorIconLeft(
            activity!!.theme, buttonLanguage,
            R.drawable.ic_language_white_24dp
        )
        ViewTools.setVectorIconLeft(
            activity!!.theme, buttonDisplaySimilar,
            R.drawable.ic_search_white_24dp
        )
        ViewTools.setVectorIconLeft(
            activity!!.theme, buttonStreamingSearch,
            R.drawable.ic_play_arrow_black_24dp
        )
        // Long press hint.
        CheatSheet.setup(buttonLanguage, R.string.pref_language)
        // Buttons.
        buttonStreamingSearch.isGone = StreamingSearch.isNotConfiguredOrTurnedOff(context!!)
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

        val displayedShow = this.displayedShow
        if (displayedShow == null) {
            // No progress, dialog will be dismissed.
            showProgressBar(false)
        } else {
            // Load show details.
            showProgressBar(true)
            val args = Bundle().apply {
                putInt(KEY_SHOW_TVDBID, displayedShow.tvdbid)
                putString(KEY_SHOW_LANGUAGE, displayedShow.language)
            }
            LoaderManager.getInstance(this)
                .initLoader(ShowsActivity.ADD_SHOW_LOADER_ID, args, showLoaderCallbacks)
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
        displayedShow?.let {
            LanguageChoiceDialogFragment.show(
                fragmentManager!!,
                R.array.languageCodesShows,
                it.language,
                LanguageChoiceDialogFragment.TAG_ADD_DIALOG
            )
        }
    }

    @OnClick(R.id.buttonAddDisplaySimilar)
    fun onClickButtonDisplaySimilarShows() {
        displayedShow?.let {
            dismissAllowingStateLoss()
            SimilarShowsFragment.displaySimilarShowsEventLiveData.postValue(it)
        }
    }

    @OnClick(R.id.buttonAddStreamingSearch)
    fun onClickButtonStreamingSearch() {
        displayedShow?.title?.let {
            StreamingSearch.searchForShow(requireContext(), it)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: LanguageChoiceDialogFragment.LanguageChangedEvent) {
        if (LanguageChoiceDialogFragment.TAG_ADD_DIALOG != event.tag) {
            return
        }

        // Reload show details with new language.
        showProgressBar(true)
        overview.visibility = View.INVISIBLE

        val displayedShow = this.displayedShow!!
        displayedShow.language = event.selectedLanguageCode
        val args = Bundle().apply {
            putInt(KEY_SHOW_TVDBID, displayedShow.tvdbid)
            putString(KEY_SHOW_LANGUAGE, displayedShow.language)
        }
        LoaderManager.getInstance(this)
            .restartLoader(ShowsActivity.ADD_SHOW_LOADER_ID, args, showLoaderCallbacks)
    }

    private fun populateShowViews(result: TvdbShowLoader.Result) {
        val displayedShow = this.displayedShow!!
        val show = result.show
        if (show == null) {
            // Failed to load, can't be added.
            if (!AndroidUtils.isNetworkConnected(activity)) {
                overview.setText(R.string.offline)
            } else if (result.doesNotExist) {
                overview.setText(R.string.tvdb_error_does_not_exist)
            } else {
                overview.text = getString(
                    R.string.api_error_generic,
                    "${getString(R.string.tvdb)}/${getString(R.string.trakt)}"
                )
            }
            return
        }
        if (result.isAdded) {
            // Already added, offer to open show instead.
            buttonPositive.setText(R.string.action_open)
            buttonPositive.setOnClickListener {
                startActivity(OverviewActivity.intentShow(context, displayedShow.tvdbid))
                dismiss()
            }
        } else {
            // Not added, offer to add.
            buttonPositive.setText(R.string.action_shows_add)
            buttonPositive.setOnClickListener {
                EventBus.getDefault().post(AddFragment.OnAddingShowEvent(displayedShow.tvdbid))
                addShowListener.onAddShow(displayedShow)
                dismiss()
            }
        }
        buttonPositive.isGone = false

        // Store title for add task.
        displayedShow.title = show.title

        buttonLanguage.text =
            LanguageTools.getShowLanguageStringFor(context, displayedShow.language)

        // Title, overview.
        title.text = show.title
        overview.text = show.overview

        // Release year.
        val statusText = SpannableStringBuilder().also { statusText ->
            TimeTools.getShowReleaseYear(show.first_aired)?.let {
                statusText.append(it)
            }
            // Continuing/ended status.
            val encodedStatus = DataLiberationTools.encodeShowStatus(show.status)
            if (encodedStatus != ShowTools.Status.UNKNOWN) {
                val decodedStatus = ShowTools.getStatus(activity!!, encodedStatus)
                if (decodedStatus != null) {
                    if (statusText.isNotEmpty()) {
                        statusText.append(" / ") // Like "2016 / Continuing".
                    }

                    val currentTextLength = statusText.length
                    statusText.append(decodedStatus)

                    // If continuing, paint status green.
                    val style = if (encodedStatus == ShowTools.Status.CONTINUING) {
                        R.style.TextAppearance_Body_Green
                    } else {
                        R.style.TextAppearance_Body_Secondary
                    }
                    statusText.setSpan(
                        TextAppearanceSpan(activity, style),
                        currentTextLength,
                        statusText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        releasedTextView.text = statusText

        // Next release day and time.
        val timeAndNetworkText = SpannableStringBuilder().apply {
            if (show.release_time != -1) {
                val release = TimeTools.getShowReleaseDateTime(
                    activity!!,
                    show.release_time,
                    show.release_weekday,
                    show.release_timezone,
                    show.country,
                    show.network
                )
                val day = TimeTools.formatToLocalDayOrDaily(activity, release, show.release_weekday)
                val time = TimeTools.formatToLocalTime(activity, release)
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
        rating.text = TraktTools.buildRatingString(show.rating)

        // Genres.
        ViewTools.setValueOrPlaceholder(genres, TextTools.splitAndKitTVDBStrings(show.genres))

        // Poster.
        TvdbImageTools.loadShowPosterFitCrop(activity, poster, show.poster_small)

        // Enable adding of show, display views.
        buttonPositive.isEnabled = true
        ViewCollections.set(labelViews, VISIBLE_SETTER, true)
    }

    private fun showProgressBar(isVisible: Boolean) {
        progressBar.isGone = !isVisible
    }

    private val showLoaderCallbacks =
        object : LoaderManager.LoaderCallbacks<TvdbShowLoader.Result> {
            override fun onCreateLoader(id: Int, args: Bundle?): Loader<TvdbShowLoader.Result> {
                val showTvdbId = args!!.getInt(KEY_SHOW_TVDBID)
                val language = args.getString(KEY_SHOW_LANGUAGE)!!
                return TvdbShowLoader(context, showTvdbId, language)
            }

            override fun onLoadFinished(
                loader: Loader<TvdbShowLoader.Result>,
                data: TvdbShowLoader.Result
            ) {
                if (!isAdded) {
                    return
                }
                showProgressBar(false)
                overview.isGone = false
                populateShowViews(data)
            }

            override fun onLoaderReset(loader: Loader<TvdbShowLoader.Result>) {
                // Do nothing.
            }
        }

    companion object {

        private const val TAG = "AddShowDialogFragment"

        private const val ARG_SEARCH_RESULT = "search_result"

        private const val KEY_SHOW_TVDBID = "show_tvdbid"
        private const val KEY_SHOW_LANGUAGE = "show_language"

        /**
         * Display a [AddShowDialogFragment] for the given show. The language of the show should
         * be set.
         */
        @JvmStatic
        fun show(context: Context, fm: FragmentManager, show: SearchResult) {
            // Replace any currently showing add dialog (do not add it to the back stack).
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            newInstance(context, show).safeShow(fm, ft, TAG)
        }

        /**
         * Display a [AddShowDialogFragment] for the given show.
         *
         * Use if there is no actual search result, but just a TheTVDB id available. Uses the search
         * or fall back language.
         */
        @JvmStatic
        fun show(context: Context, fm: FragmentManager, showTvdbId: Int) {
            val fakeResult = SearchResult().apply {
                tvdbid = showTvdbId
            }
            show(context, fm, fakeResult)
        }

        private fun newInstance(context: Context, show: SearchResult): AddShowDialogFragment {
            if (TextUtils.isEmpty(show.language)) {
                // Use search or fall back language.
                show.language = DisplaySettings.getSearchLanguageOrFallbackIfAny(context)
            }

            return AddShowDialogFragment().apply { 
                arguments = Bundle().apply {
                    putParcelable(ARG_SEARCH_RESULT, show)
                }
            }
        }

        val VISIBLE_SETTER = Setter<View, Boolean> { view, value, _ ->
            view.visibility = if (value == true) View.VISIBLE else View.INVISIBLE
        }
    }
}
