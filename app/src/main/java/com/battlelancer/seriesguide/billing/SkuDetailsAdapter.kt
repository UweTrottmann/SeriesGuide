package com.battlelancer.seriesguide.billing

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
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
        private val skuTitle: TextView = itemView.findViewById(R.id.sku_title)
        private val skuPrice: TextView = itemView.findViewById(R.id.sku_price)
        private val skuDescription: TextView = itemView.findViewById(R.id.sku_description)

        init {
            itemView.setOnClickListener {
                getItem(adapterPosition)?.let { onSkuDetailsClicked(it) }
            }
        }

        fun bind(item: AugmentedSkuDetails?) {
            item?.apply {
                itemView.apply {
                    val name = title?.substring(0, title!!.indexOf("("))
                    skuTitle.text = name
                    skuDescription.text = description
                    skuPrice.text = price
                    isEnabled = canPurchase
                    onDisabled(canPurchase)
                }
            }
        }

        private fun onDisabled(enabled: Boolean) {
            if (enabled) {
                itemView.apply {
                    TextViewCompat.setTextAppearance(skuTitle, R.style.TextAppearance_Body)
                    TextViewCompat.setTextAppearance(skuPrice, R.style.TextAppearance_Body)
                    TextViewCompat.setTextAppearance(skuDescription, R.style.TextAppearance_Body)
                }
            } else {
                itemView.apply {
                    TextViewCompat.setTextAppearance(skuTitle, R.style.TextAppearance_Body_Dim)
                    TextViewCompat.setTextAppearance(skuPrice, R.style.TextAppearance_Body_Dim)
                    TextViewCompat.setTextAppearance(
                        skuDescription,
                        R.style.TextAppearance_Body_Dim
                    )
                }
            }
        }
    }
}