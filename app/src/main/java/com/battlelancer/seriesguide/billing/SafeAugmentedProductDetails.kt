// SPDX-License-Identifier: Apache-2.0
// Copyright 2023-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import com.android.billingclient.api.ProductDetails

/**
 * Like [AugmentedProductDetails], but with some values guaranteed to be not null.
 */
data class SafeAugmentedProductDetails(
    val productId: String,
    val canPurchase: Boolean,
    val productDetails: ProductDetails,
    val pricingPhases: List<ProductDetails.PricingPhase>
)
