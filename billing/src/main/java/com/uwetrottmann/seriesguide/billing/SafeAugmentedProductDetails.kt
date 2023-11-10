// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.uwetrottmann.seriesguide.billing

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
