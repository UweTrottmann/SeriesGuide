package com.battlelancer.seriesguide.billing.amazon;

/**
 * Contains all Amazon in-app purchase products definitions.
 */
public enum AmazonSku {

    /**
     * The unlock all subscription parent SKU.
     *
     * <p>This SKU is used in receipts (the actual purchased period SKU is NOT used, instead the
     * purchase and cancel date of the receipt are populated).
     */
    SERIESGUIDE_SUB_PARENT("seriesguide-sub"),
    /**
     * The unlock all subscription yearly period SKU with a trial month.
     *
     * <p>{@code seriesguide-sub} is the parent SKU, the unlock all subscription. It may have
     * multiple children with different periods. Currently we only have one yearly period.
     */
    SERIESGUIDE_SUB_YEARLY("seriesguide-sub-year"),
    /**
     * The one-time purchase. Unlocks access to everything.
     */
    SERIESGUIDE_PASS("x-pass-2014-10");

    private final String sku;

    public String getSku() {
        return this.sku;
    }

    AmazonSku(final String sku) {
        this.sku = sku;
    }

    public static AmazonSku fromSku(final String sku) {
        if (SERIESGUIDE_SUB_PARENT.getSku().equals(sku)) {
            return SERIESGUIDE_SUB_PARENT;
        }
        if (SERIESGUIDE_SUB_YEARLY.getSku().equals(sku)) {
            return SERIESGUIDE_SUB_YEARLY;
        }
        if (SERIESGUIDE_PASS.getSku().equals(sku)) {
            return SERIESGUIDE_PASS;
        }
        return null;
    }

}
