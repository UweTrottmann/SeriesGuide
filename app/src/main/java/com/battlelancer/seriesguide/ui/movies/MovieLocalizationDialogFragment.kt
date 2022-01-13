package com.battlelancer.seriesguide.ui.movies

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogLocalizationBinding
import com.battlelancer.seriesguide.databinding.ItemDropdownBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.movies.MovieLocalizationDialogFragment.LocalizationAdapter.LocalizationItem
import com.battlelancer.seriesguide.ui.movies.MovieLocalizationDialogFragment.LocalizationChangedEvent
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.Collator
import java.util.ArrayList
import java.util.Locale

/**
 * A dialog displaying a list of languages and regions to choose from, posting a
 * [LocalizationChangedEvent] once the dialog is dismissed (even if language or region
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

    private var _binding: DialogLocalizationBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LocalizationAdapter
    private var currentCodeType: CodeType = CodeType.Language

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogLocalizationBinding.inflate(layoutInflater)

        binding.buttonDismiss.apply {
            setText(R.string.dismiss)
            setOnClickListener { dismiss() }
        }

        adapter = LocalizationAdapter(onItemClickListener)
        binding.recyclerViewLocalization.adapter = adapter
        binding.recyclerViewLocalization.layoutManager = LinearLayoutManager(context)

        updateButtonText()

        binding.buttonLocalizationLanguage.setOnClickListener {
            adapter.updateItems(emptyList())
            setListVisible(true)
            lifecycleScope.launch {
                val languageCodes = requireContext().resources
                    .getStringArray(R.array.languageCodesMovies)
                val items: MutableList<LocalizationItem> = ArrayList(languageCodes.size)

                for (languageCode in languageCodes) {
                    items.add(
                        LocalizationItem(
                            languageCode,
                            LanguageTools.buildLanguageDisplayName(languageCode)
                        )
                    )
                }

                val collator = Collator.getInstance()
                items.sortWith { left: LocalizationItem, right: LocalizationItem ->
                    collator.compare(left.displayText, right.displayText)
                }

                EventBus.getDefault().postSticky(ItemsLoadedEvent(items, CodeType.Language))
            }
        }
        binding.buttonLocalizationRegion.setOnClickListener {
            adapter.updateItems(emptyList())
            setListVisible(true)
            lifecycleScope.launch {
                val regionCodes = Locale.getISOCountries()
                val items: MutableList<LocalizationItem> = ArrayList(regionCodes.size)

                for (regionCode in regionCodes) {
                    // example: "en-US"
                    items.add(
                        LocalizationItem(
                            regionCode,
                            Locale("", regionCode).displayCountry
                        )
                    )
                }

                val collator = Collator.getInstance()
                items.sortWith { left: LocalizationItem, right: LocalizationItem ->
                    collator.compare(left.displayText, right.displayText)
                }

                EventBus.getDefault().postSticky(ItemsLoadedEvent(items, CodeType.Region))
            }
        }

        restoreViewState(savedInstanceState)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun restoreViewState(savedInstanceState: Bundle?) {
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
            binding.recyclerViewLocalization.visibility == View.VISIBLE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        EventBus.getDefault().post(LocalizationChangedEvent())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventItemsLoaded(event: ItemsLoadedEvent) {
        binding.recyclerViewLocalization.scrollToPosition(0)
        currentCodeType = event.type
        adapter.updateItems(event.items)
    }

    private fun updateButtonText() {
        // example: "en-US"
        val languageCode = DisplaySettings.getMoviesLanguage(requireContext())
        binding.buttonLocalizationLanguage.text =
            LanguageTools.buildLanguageDisplayName(languageCode)
        val regionCode = DisplaySettings.getMoviesRegion(requireContext())
        binding.buttonLocalizationRegion.text = Locale("", regionCode).displayCountry
    }

    private fun setListVisible(listIsVisible: Boolean) {
        binding.recyclerViewLocalization.isGone = !listIsVisible

        binding.buttonLocalizationLanguage.isGone = listIsVisible
        binding.textViewLocalizationLanguage.isGone = listIsVisible
        binding.buttonLocalizationRegion.isGone = listIsVisible
        binding.textViewLocalizationRegion.isGone = listIsVisible
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

        @SuppressLint("NotifyDataSetChanged")
        fun updateItems(items: List<LocalizationItem>) {
            this.items.clear()
            this.items.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return ViewHolder(
                ItemDropdownBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), onItemClickListener
            )
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int
        ) {
            if (holder is ViewHolder) {
                val item = items[position]
                holder.code = item.code
                holder.binding.text1.text = item.displayText
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(
            val binding: ItemDropdownBinding,
            onItemClickListener: OnItemClickListener
        ) : RecyclerView.ViewHolder(binding.root) {

            var code: String? = null

            init {
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