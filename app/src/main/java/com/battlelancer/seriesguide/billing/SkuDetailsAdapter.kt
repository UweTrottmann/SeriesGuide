package com.battlelancer.seriesguide.billing

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.uwetrottmann.seriesguide.billing.SafeAugmentedProductDetails

/**
 * This is a [SafeAugmentedProductDetails] adapter base class to show available products.
 * It highlights purchased products and shows if a free trial is available for a product.
 */
open class SkuDetailsAdapter : RecyclerView.Adapter<SkuDetailsAdapter.ProductViewHolder>() {

    private var skuDetailsList = emptyList<SafeAugmentedProductDetails>()

    override fun getItemCount() = skuDetailsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItem(position: Int) = if (skuDetailsList.isEmpty()) null else skuDetailsList[position]

    @SuppressLint("NotifyDataSetChanged") // List is short, no point in only updating changed.
    fun setProductDetailsList(list: List<SafeAugmentedProductDetails>) {
        if (list != skuDetailsList) {
            skuDetailsList = list
            notifyDataSetChanged()
        }
    }

    /**
     * Called when a product that can be purchased is clicked.
     */
    open fun onSkuDetailsClicked(item: SafeAugmentedProductDetails) {
        //clients to implement for callback if needed
    }

    inner class ProductViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
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

        fun bind(item: SafeAugmentedProductDetails?) {
            if (item == null) {
                skuTitle.text = null
                skuDescription.text = null
                skuPrice.text = null
                itemView.isEnabled = false
                onDisabled(enabled = true)
                return
            }
            val productDetails = item.productDetails
            skuTitle.text = productDetails.name // title includes app name, so use name.
            skuDescription.text = productDetails.description

            val offerToDisplay = item.pricingPhases.maxBy { it.priceAmountMicros }
            val trialAvailable = item.pricingPhases.find { it.priceAmountMicros == 0L } != null
            var priceText = itemView.context.getString(
                R.string.billing_duration_format,
                offerToDisplay.formattedPrice
            )
            if (trialAvailable) {
                priceText += "\n${itemView.context.getString(R.string.billing_free_trial)}"
            }
            skuPrice.text = priceText

            itemView.isEnabled = item.canPurchase
            onDisabled(item.canPurchase)
        }

        private fun onDisabled(enabled: Boolean) {
            if (enabled) {
                itemView.apply {
                    // Subscription for purchase.
                    skuImage.isGone = true
                    TextViewCompat.setTextAppearance(
                        skuTitle,
                        R.style.TextAppearance_SeriesGuide_Body2_Bold
                    )
                    TextViewCompat.setTextAppearance(
                        skuPrice,
                        R.style.TextAppearance_SeriesGuide_Body2_Bold
                    )
                    TextViewCompat.setTextAppearance(
                        skuDescription,
                        R.style.TextAppearance_SeriesGuide_Body2
                    )
                }
            } else {
                itemView.apply {
                    // Subscription is active.
                    skuImage.isGone = false
                    TextViewCompat.setTextAppearance(
                        skuTitle,
                        R.style.TextAppearance_SeriesGuide_Body2_Bold_Dim
                    )
                    TextViewCompat.setTextAppearance(
                        skuPrice,
                        R.style.TextAppearance_SeriesGuide_Body2_Bold_Dim
                    )
                    TextViewCompat.setTextAppearance(
                        skuDescription,
                        R.style.TextAppearance_SeriesGuide_Body2_Dim
                    )
                }
            }
        }
    }
}