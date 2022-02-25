package com.battlelancer.seriesguide.ui.episodes

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemEpisodeBinding
import com.battlelancer.seriesguide.provider.SgEpisode2Info
import com.battlelancer.seriesguide.settings.DisplaySettings.isDisplayExactDate
import com.battlelancer.seriesguide.settings.DisplaySettings.preventSpoilers
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.widgets.WatchedBox
import java.text.NumberFormat


class EpisodesAdapter(
    private val context: Context,
    private val clickListener: ClickListener
) : ListAdapter<SgEpisode2Info, EpisodeViewHolder>(SgEpisode2InfoDiffCallback) {

    var selectedItemId: Long = -1

    init {
        setHasStableIds(true)
    }

    // Is called (indirectly) by external code, so do size check.
    override fun getItemId(position: Int): Long =
        currentList.getOrNull(position)?.id ?: RecyclerView.NO_ID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder.create(parent, clickListener)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val item: SgEpisode2Info? = getItem(position)
        val isSelected = item?.id == selectedItemId
        holder.bind(item, isSelected, context)
    }

    /**
     * Returns -1 if the item ID was not found in the [getCurrentList].
     */
    fun getPositionForId(itemId: Long): Int {
        return currentList.indexOfFirst { it.id == itemId }
    }

    fun selectItem(position: Int): Long {
        val previousSelectedPosition = selectedItemId
            .let { if (it != -1L) getPositionForId(it) else -1 }
        val selectedItemId = getItemId(position)
            .also { selectedItemId = it }
        if (previousSelectedPosition != -1) notifyItemChanged(previousSelectedPosition)
        notifyItemChanged(position)
        return selectedItemId
    }

    interface ClickListener {
        fun onItemClick(position: Int)
        fun onWatchedBoxClick(episodeId: Long, isWatched: Boolean)
        fun onPopupMenuClick(
            v: View, episodeId: Long, episodeNumber: Int,
            releaseTimeMs: Long, watchedFlag: Int, isCollected: Boolean
        )
    }

}

object SgEpisode2InfoDiffCallback : DiffUtil.ItemCallback<SgEpisode2Info>() {
    override fun areItemsTheSame(oldItem: SgEpisode2Info, newItem: SgEpisode2Info): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: SgEpisode2Info, newItem: SgEpisode2Info): Boolean =
        oldItem == newItem
}

class EpisodeViewHolder(
    private val binding: ItemEpisodeBinding,
    clickListener: EpisodesAdapter.ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private val integerFormat = NumberFormat.getIntegerInstance()
    private var episode: SgEpisode2Info? = null

    init {
        binding.root.setOnClickListener {
            clickListener.onItemClick(absoluteAdapterPosition)
        }
        binding.watchedBoxEpisode.setOnClickListener { view ->
            episode?.let {
                val box = view as WatchedBox
                // disable button, will be re-enabled on data reload once action completes
                box.isEnabled = false
                clickListener.onWatchedBoxClick(it.id, !EpisodeTools.isWatched(box.episodeFlag))
            }
        }
        binding.imageViewContextMenu.setOnClickListener { v ->
            episode?.let {
                clickListener.onPopupMenuClick(
                    v,
                    it.id,
                    it.episodenumber,
                    it.firstReleasedMs,
                    it.watched,
                    it.collected
                )
            }
        }
    }

    fun bind(episode: SgEpisode2Info?, isActivated: Boolean, context: Context) {
        this.episode = episode

        // selection state
        binding.root.isActivated = isActivated

        if (episode == null) {
            binding.textViewEpisodeTitle.text = null
            binding.textViewEpisodeNumber.text = null
            binding.watchedBoxEpisode.episodeFlag = EpisodeFlags.UNWATCHED
            binding.watchedBoxEpisode.isEnabled = false
            binding.imageViewCollected.visibility = View.INVISIBLE
            binding.textViewEpisodeAlternativeNumbers.text = null
            binding.textViewEpisodeAirdate.text = null
            return
        }

        // episode title
        val watchedFlag = episode.watched
        val episodeNumber = episode.episodenumber
        val hideTitle = (EpisodeTools.isUnwatched(watchedFlag) && preventSpoilers(context))
        binding.textViewEpisodeTitle.text = TextTools.getEpisodeTitle(
            context, if (hideTitle) null else episode.title, episodeNumber
        )

        // number
        binding.textViewEpisodeNumber.text = integerFormat.format(episodeNumber.toLong())

        // watched box
        binding.watchedBoxEpisode.episodeFlag = watchedFlag
        binding.watchedBoxEpisode.isEnabled = true
        val watched = EpisodeTools.isWatched(watchedFlag)
        binding.watchedBoxEpisode.contentDescription =
            context.getString(if (watched) R.string.action_unwatched else R.string.action_watched)
        TooltipCompat.setTooltipText(
            binding.watchedBoxEpisode,
            binding.watchedBoxEpisode.context.getString(
                if (watched) R.string.action_unwatched else R.string.action_watched
            )
        )

        // collected tag
        val isCollected = episode.collected
        binding.imageViewCollected.visibility = if (isCollected) View.VISIBLE else View.INVISIBLE

        // alternative numbers
        val absoluteNumber = episode.absoluteNumber
        val absoluteNumberText = if (absoluteNumber > 0) {
            integerFormat.format(absoluteNumber.toLong())
        } else null
        val dvdNumber = episode.dvdNumber
        val dvdNumberText: String? = if (dvdNumber > 0) {
            "${context.getString(R.string.episode_number_disk)} $dvdNumber"
        } else null
        binding.textViewEpisodeAlternativeNumbers.text =
            TextTools.dotSeparate(absoluteNumberText, dvdNumberText)

        // release time
        val isReleased: Boolean
        val releaseTime = episode.firstReleasedMs
        if (releaseTime != -1L) {
            val actualRelease = TimeTools.applyUserOffset(context, releaseTime)
            isReleased = TimeTools.isReleased(actualRelease)
            // "in 15 mins" or "Oct 31, 2010"
            val displayExactDate = isDisplayExactDate(context)
            binding.textViewEpisodeAirdate.text =
                if (displayExactDate) TimeTools.formatToLocalDateShort(
                    context,
                    actualRelease
                ) else TimeTools.formatToLocalRelativeTime(context, actualRelease)
        } else {
            binding.textViewEpisodeAirdate.text =
                context.getString(R.string.episode_firstaired_unknown)
            isReleased = false
        }

        // dim text color if not released
        TextViewCompat.setTextAppearance(
            binding.textViewEpisodeTitle,
            if (isReleased) {
                R.style.TextAppearance_SeriesGuide_Subtitle1_Bold
            } else R.style.TextAppearance_SeriesGuide_Subtitle1_Bold_Dim
        )
        TextViewCompat.setTextAppearance(
            binding.textViewEpisodeAirdate,
            if (isReleased) {
                R.style.TextAppearance_SeriesGuide_Body2_Secondary
            } else R.style.TextAppearance_SeriesGuide_Body2_Dim
        )
    }

    companion object {
        fun create(
            parent: ViewGroup,
            clickListener: EpisodesAdapter.ClickListener
        ): EpisodeViewHolder {
            return EpisodeViewHolder(
                ItemEpisodeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                clickListener
            )
        }
    }
}
