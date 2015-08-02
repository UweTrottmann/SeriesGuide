/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.billing.amazon;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.RequestId;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.Utils;
import timber.log.Timber;

public class AmazonBillingActivity extends BaseActivity {

    @Bind(R.id.progressBarAmazonBilling) View progressBar;
    @Bind(R.id.buttonAmazonBillingSubscribe) Button buttonSubscribe;
    @Bind(R.id.textViewAmazonBillingSubPrice) TextView textViewPriceSub;
    @Bind(R.id.buttonAmazonBillingGetPass) Button buttonGetPass;
    @Bind(R.id.textViewAmazonBillingPricePass) TextView textViewPricePass;
    @Bind(R.id.textViewAmazonBillingExisting) TextView textViewIsSupporter;
    @Bind(R.id.buttonAmazonBillingDismiss) Button buttonDismiss;
    @Bind(R.id.textViewAmazonBillingMoreInfo) View buttonMoreInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amazon_billing);
        setupActionBar();

        setupViews();

        AmazonIapManager.setup(this);
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        ButterKnife.bind(this);

        buttonSubscribe.setEnabled(false);
        buttonSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribe();
            }
        });

        buttonGetPass.setEnabled(false);
        buttonGetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                purchasePass();
            }
        });

        buttonDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        buttonMoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.launchWebsite(v.getContext(), getString(R.string.url_whypay),
                        "AmazonBillingActivity", "WhyPayWebsite");
            }
        });

        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // no need to get product data every time we were hidden, so do it in onStart
        AmazonIapManager.get().requestProductData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AmazonIapManager.get().activate();
        AmazonIapManager.get().requestUserDataAndPurchaseUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        AmazonIapManager.get().deactivate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }

    private void subscribe() {
        final RequestId requestId = PurchasingService.purchase(AmazonSku.SERIESGUIDE_SUB.getSku());
        Timber.d("subscribe: requestId (" + requestId + ")");
    }

    private void purchasePass() {
        final RequestId requestId = PurchasingService.purchase(
                AmazonSku.SERIESGUIDE_PASS.getSku());
        Timber.d("purchasePass: requestId (" + requestId + ")");
    }

    private void dismiss() {
        finish();
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapMessageEvent event) {
        Toast.makeText(this, event.messageResId, Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapAvailabilityEvent event) {
        if (progressBar == null || buttonSubscribe == null || buttonGetPass == null
                || textViewIsSupporter == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);

        // enable or disable purchase buttons based on what can be purchased
        buttonSubscribe.setEnabled(event.subscriptionAvailable && !event.userHasActivePurchase);
        buttonGetPass.setEnabled(event.passAvailable && !event.userHasActivePurchase);

        // status text
        if (!event.subscriptionAvailable && !event.passAvailable) {
            // neither purchase available, probably not signed in
            textViewIsSupporter.setText(R.string.subscription_not_signed_in);
        } else {
            // subscription or pass available
            // show message if either one is active
            textViewIsSupporter.setText(
                    event.userHasActivePurchase ? getString(R.string.upgrade_success) : null);
        }
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapProductEvent event) {
        Product product = event.product;
        // display the actual price like "1.23 C"
        String price = product.getPrice();
        if (price == null) {
            price = "--";
        }
        if (AmazonSku.SERIESGUIDE_SUB.getSku().equals(product.getSku())) {
            if (textViewPriceSub != null) {
                textViewPriceSub.setText(
                        getString(R.string.billing_price_subscribe, price));
            }
            return;
        }
        if (AmazonSku.SERIESGUIDE_PASS.getSku().equals(product.getSku())) {
            if (textViewPricePass != null) {
                textViewPricePass.setText(
                        price + "\n" + getString(R.string.billing_price_pass));
            }
        }
    }
}
