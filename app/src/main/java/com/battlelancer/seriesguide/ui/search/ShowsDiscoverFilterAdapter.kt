package com.battlelancer.seriesguide.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemWatchProviderBinding
import com.battlelancer.seriesguide.model.SgWatchProvider

class ShowsDiscoverFilterAdapter(
    private val clickListener: ClickListener
) : PagingDataAdapter<SgWatchProvider, SgWatchProviderViewHolder>(
    SgWatchProviderDiffCallback
) {
    interface ClickListener {
        fun onClick(watchProvider: SgWatchProvider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SgWatchProviderViewHolder {
        return SgWatchProviderViewHolder(
            ItemWatchProviderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            clickListener
        )
    }

    override fun onBindViewHolder(holder: SgWatchProviderViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }
}

object SgWatchProviderDiffCallback : DiffUtil.ItemCallback<SgWatchProvider>() {
    override fun areItemsTheSame(oldItem: SgWatchProvider, newItem: SgWatchProvider): Boolean =
        oldItem._id == newItem._id

    override fun areContentsTheSame(oldItem: SgWatchProvider, newItem: SgWatchProvider): Boolean =
        oldItem == newItem
}

class SgWatchProviderViewHolder(
    private val binding: ItemWatchProviderBinding,
    clickListener: ShowsDiscoverFilterAdapter.ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var watchProvider: SgWatchProvider? = null

    init {
        binding.switchWatchProvider.setOnClickListener {
            watchProvider?.let {
                clickListener.onClick(it)
            }
        }
    }

    fun bindTo(watchProvider: SgWatchProvider?) {
        this.watchProvider = watchProvider
        if (watchProvider == null) {
            binding.switchWatchProvider.apply {
                text = null
                isChecked = false
                isEnabled = false
            }
        } else {
            binding.switchWatchProvider.apply {
                text = watchProvider.provider_name
                isChecked = watchProvider.enabled
                isEnabled = true
            }
        }
    }
}
