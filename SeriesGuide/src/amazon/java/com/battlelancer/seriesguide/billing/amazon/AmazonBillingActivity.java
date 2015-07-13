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
import com.amazon.device.iap.model.RequestId;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.Utils;
import timber.log.Timber;

public class AmazonBillingActivity extends BaseActivity {

    @Bind(R.id.progressBarAmazonBilling) View progressBar;
    @Bind(R.id.buttonAmazonBillingSubscribe) Button buttonSubscribe;
    @Bind(R.id.textViewAmazonBillingPrice) TextView textViewSubPrice;
    @Bind(R.id.textViewAmazonBillingExisting) TextView textViewSubscribed;
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

    private void dismiss() {
        finish();
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapMessageEvent event) {
        Toast.makeText(this, event.messageResId, Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapAvailabilityEvent event) {
        if (progressBar == null || buttonSubscribe == null || textViewSubscribed == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);

        boolean isSubscribed = event.productAvailable && !event.userCanSubscribe;

        // subscribe button
        buttonSubscribe.setEnabled(event.productAvailable && event.userCanSubscribe);

        // status text
        if (!event.productAvailable) {
            textViewSubscribed.setText(R.string.subscription_not_signed_in);
        } else {
            textViewSubscribed.setText(
                    isSubscribed ? R.string.upgrade_success : R.string.subscription_expired);
        }
    }

    public void onEventMainThread(AmazonIapManager.AmazonIapPriceEvent event) {
        if (textViewSubPrice != null) {
            // display price like "1.23 C"
            textViewSubPrice.setText(
                    getString(R.string.billing_price_subscribe, event.product.getPrice()));
        }
    }
}
