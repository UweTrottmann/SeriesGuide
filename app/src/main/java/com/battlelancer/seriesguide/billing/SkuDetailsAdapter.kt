package com.battlelancer.seriesguide.billing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails

/**
 * This is an [AugmentedSkuDetails] adapter. It can be used anywhere there is a need to display a
 * list of AugmentedSkuDetails. In this app it's used to display both the list of subscriptions and
 * the list of in-app products.
 */
open class SkuDetailsAdapter : RecyclerView.Adapter<SkuDetailsAdapter.SkuDetailsViewHolder>() {

    private var skuDetailsList = emptyList<AugmentedSkuDetails>()

    override fun getItemCount() = skuDetailsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkuDetailsViewHolder {
        return SkuDetailsViewHolder(parent)
    }

    override fun onBindViewHolder(holder: SkuDetailsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItem(position: Int) = if (skuDetailsList.isEmpty()) null else skuDetailsList[position]

    fun setSkuDetailsList(list: List<AugmentedSkuDetails>) {
        if (list != skuDetailsList) {
            skuDetailsList = list
            notifyDataSetChanged()
        }
    }

    /**
     * In the spirit of keeping simple things simple: this is a friendly way of allowing clients
     * to listen to clicks. You should consider doing this for all your other adapters.
     */
    open fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
        //clients to implement for callback if needed
    }

    inner class SkuDetailsViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.item_inventory,
            parent,
            false
        )
    ) {
        private val skuImage: View = itemView.findViewById(R.id.sku_image)
        private val skuTitle: TextView = itemView.findViewById(R.id.sku_title)
        private val skuPrice: TextView = itemView.findViewById(R.id.sku_price)
        private val skuDescription: TextView = itemView.findViewById(R.id.sku_description)

        init {
            itemView.setOnClickListener {
                getItem(bindingAdapterPosition)?.let { onSkuDetailsClicked(it) }
            }
        }

        fun bind(item: AugmentedSkuDetails?) {
            item?.apply {
                itemView.apply {
                    val name = title?.let {
                        // In most cases subscription title is followed by app name in (), remove it.
                        val indexOfAppName = it.indexOf("(")
                        if (indexOfAppName != -1) {
                            it.substring(0, indexOfAppName)
                        } else {
                            it
                        }
                    }
                    skuTitle.text = name
                    skuDescription.text = description
                    skuPrice.text = context.getString(R.string.billing_duration_format, price)
                    isEnabled = canPurchase
                    onDisabled(canPurchase)
                }
            }
        }

        private fun onDisabled(enabled: Boolean) {
            if (enabled) {
                itemView.apply {
                    // Subscription for purchase.
                    skuImage.isGone = true
                    TextViewCompat.setTextAppearance(skuTitle, R.style.TextAppearance_SeriesGuide_Body2_Bold)
                    TextViewCompat.setTextAppearance(skuPrice, R.style.TextAppearance_SeriesGuide_Body2_Bold)
                    TextViewCompat.setTextAppearance(skuDescription, R.style.TextAppearance_SeriesGuide_Body2)
                }
            } else {
                itemView.apply {
                    // Subscription is active.
                    skuImage.isGone = false
                    TextViewCompat.setTextAppearance(skuTitle, R.style.TextAppearance_SeriesGuide_Body2_Bold_Dim)
                    TextViewCompat.setTextAppearance(skuPrice, R.style.TextAppearance_SeriesGuide_Body2_Bold_Dim)
                    TextViewCompat.setTextAppearance(
                        skuDescription,
                        R.style.TextAppearance_SeriesGuide_Body2_Dim
                    )
                }
            }
        }
    }
}