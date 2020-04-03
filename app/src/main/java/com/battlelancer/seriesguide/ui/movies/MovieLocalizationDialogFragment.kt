package com.battlelancer.seriesguide.ui.movies

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.movies.MovieLocalizationDialogFragment.LocalizationAdapter.LocalizationItem
import com.battlelancer.seriesguide.util.safeShow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.Collator
import java.util.ArrayList
import java.util.Locale

/**
 * A dialog displaying a list of languages and regions to choose from, posting a [ ] once the dialog is dismissed (even if language or region
 * have not changed).
 */
class MovieLocalizationDialogFragment : AppCompatDialogFragment() {

    class LocalizationChangedEvent

    sealed class CodeType {
        object Language : CodeType()
        object Region : CodeType()
    }

    data class ItemsLoadedEvent(
        val items: List<LocalizationItem>,
        val type: CodeType
    )

    private var unbinder: Unbinder? = null

    @BindView(R.id.buttonPositive)
    var buttonOk: Button? = null

    @BindView(R.id.recyclerViewLocalization)
    var recyclerView: RecyclerView? = null

    @BindView(R.id.textViewLocalizationLanguage)
    var textViewLanguage: TextView? = null

    @BindView(R.id.textViewLocalizationRegion)
    var textViewRegion: TextView? = null

    @BindView(R.id.buttonLocalizationLanguage)
    var buttonLanguage: Button? = null

    @BindView(R.id.buttonLocalizationRegion)
    var buttonRegion: Button? = null

    private lateinit var adapter: LocalizationAdapter
    private var currentCodeType: CodeType = CodeType.Language

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_localization, container, false)
        unbinder = ButterKnife.bind(this, view)

        buttonOk!!.setText(android.R.string.ok)
        buttonOk!!.setOnClickListener { dismiss() }

        adapter = LocalizationAdapter(onItemClickListener)
        recyclerView!!.adapter = adapter
        recyclerView!!.layoutManager = LinearLayoutManager(context)

        updateButtonText()

        buttonLanguage!!.setOnClickListener {
            setListVisible(true)
            Thread(Runnable {
                val languageCodes = requireContext().resources
                    .getStringArray(R.array.languageCodesMovies)
                val items: MutableList<LocalizationItem> = ArrayList(languageCodes.size)
                for (languageCode in languageCodes) {
                    items.add(
                        LocalizationItem(languageCode, buildLanguageDisplayName(languageCode))
                    )
                }
                val collator = Collator.getInstance()
                items.sortWith(Comparator { left: LocalizationItem, right: LocalizationItem ->
                    collator.compare(left.displayText, right.displayText)
                })
                EventBus.getDefault().postSticky(ItemsLoadedEvent(items, CodeType.Language))
            }).run()
        }
        buttonRegion!!.setOnClickListener {
            setListVisible(true)
            Thread(Runnable {
                val regionCodes = Locale.getISOCountries()
                val items: MutableList<LocalizationItem> = ArrayList(regionCodes.size)
                for (regionCode in regionCodes) {
                    // example: "en-US"
                    val displayCountry = Locale("", regionCode).displayCountry
                    items.add(
                        LocalizationItem(regionCode, displayCountry)
                    )
                }
                val collator = Collator.getInstance()
                items.sortWith(Comparator { left: LocalizationItem, right: LocalizationItem ->
                    collator.compare(left.displayText, right.displayText)
                })
                EventBus.getDefault().postSticky(ItemsLoadedEvent(items, CodeType.Region))
            }).run()
        }

        return view
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState == null) {
            setListVisible(false)
        } else {
            setListVisible(
                savedInstanceState.getBoolean(STATE_LIST_VISIBLE, false)
            )
        }
        val loadedEvent = EventBus.getDefault().getStickyEvent(
            ItemsLoadedEvent::class.java
        )
        if (loadedEvent != null) {
            adapter.updateItems(loadedEvent.items)
            currentCodeType = loadedEvent.type
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            STATE_LIST_VISIBLE,
            recyclerView!!.visibility == View.VISIBLE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        EventBus.getDefault().post(LocalizationChangedEvent())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventItemsLoaded(event: ItemsLoadedEvent) {
        recyclerView!!.scrollToPosition(0)
        currentCodeType = event.type
        adapter.updateItems(event.items)
    }

    private fun updateButtonText() {
        // example: "en-US"
        val languageCode = DisplaySettings.getMoviesLanguage(context)
        buttonLanguage!!.text = buildLanguageDisplayName(languageCode)
        val regionCode = DisplaySettings.getMoviesRegion(context)
        buttonRegion!!.text = Locale("", regionCode).displayCountry
    }

    private fun buildLanguageDisplayName(languageCode: String): String {
        // Example: "en-US".
        return if ("pt-BR" == languageCode || "pt-PT" == languageCode) {
            // Display country only for Portuguese.
            // Most other TMDB region codes are superfluous or make no sense
            // (report to TMDB?).
            Locale(languageCode.substring(0, 2), languageCode.substring(3, 5))
                .displayName
        } else {
            Locale(languageCode.substring(0, 2), "")
                .displayName
        }
    }

    private fun setListVisible(visible: Boolean) {
        recyclerView!!.visibility = if (visible) View.VISIBLE else View.GONE
        val visibility = if (visible) View.GONE else View.VISIBLE
        buttonLanguage!!.visibility = visibility
        textViewLanguage!!.visibility = visibility
        buttonRegion!!.visibility = visibility
        textViewRegion!!.visibility = visibility
    }

    private val onItemClickListener: LocalizationAdapter.OnItemClickListener =
        object : LocalizationAdapter.OnItemClickListener {
            override fun onItemClick(code: String?) {
                setListVisible(false)
                when (currentCodeType) {
                    CodeType.Language -> {
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                            putString(DisplaySettings.KEY_MOVIES_LANGUAGE, code)
                        }
                    }
                    CodeType.Region -> {
                        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                            putString(DisplaySettings.KEY_MOVIES_REGION, code)
                        }
                    }
                }
                updateButtonText()
            }
        }

    class LocalizationAdapter(val onItemClickListener: OnItemClickListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        data class LocalizationItem(
            val code: String?,
            val displayText: String
        )

        interface OnItemClickListener {
            fun onItemClick(code: String?)
        }

        private val items = ArrayList<LocalizationItem>()

        fun updateItems(items: List<LocalizationItem>) {
            this.items.clear()
            this.items.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dropdown, parent, false)
                .let { ViewHolder(it, onItemClickListener) }
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int
        ) {
            if (holder is ViewHolder) {
                val item = items[position]
                holder.code = item.code
                holder.title!!.text = item.displayText
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(
            itemView: View,
            onItemClickListener: OnItemClickListener
        ) : RecyclerView.ViewHolder(itemView) {

            @BindView(android.R.id.text1)
            var title: TextView? = null
            var code: String? = null

            init {
                ButterKnife.bind(this, itemView)
                itemView.setOnClickListener {
                    onItemClickListener.onItemClick(code)
                }
            }
        }
    }

    companion object {

        private const val STATE_LIST_VISIBLE = "listVisible"

        @JvmStatic
        fun show(fragmentManager: FragmentManager) {
            MovieLocalizationDialogFragment().safeShow(fragmentManager, "movieLanguageDialog")
        }
    }
}