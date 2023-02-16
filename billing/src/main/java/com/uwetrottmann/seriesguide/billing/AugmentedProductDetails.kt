package com.uwetrottmann.seriesguide.billing

import com.android.billingclient.api.ProductDetails

/**
 * Details about a product supported for purchase. To determine if the product should be offered for
 * purchase
 * - check [canPurchase] (the product is not already purchased on Google Play) and that
 * - [productDetails] is not null (the product is available for purchase on Google Play).
 */
data class AugmentedProductDetails(
    val productId: String,
    val canPurchase: Boolean,
    val productDetails: ProductDetails?
)
