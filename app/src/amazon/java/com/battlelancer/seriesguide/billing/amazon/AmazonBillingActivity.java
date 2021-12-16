package com.battlelancer.seriesguide.billing.amazon;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.RequestId;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ActivityAmazonBillingBinding;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.Utils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

public class AmazonBillingActivity extends BaseActivity {

    private ActivityAmazonBillingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAmazonBillingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupActionBar();

        setupViews();

        AmazonHelper.create(this);
        AmazonHelper.getIapManager().register();
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViews() {
        binding.buttonAmazonBillingSubscribe.setEnabled(false);
        binding.buttonAmazonBillingSubscribe.setOnClickListener(v -> subscribe());

        binding.buttonAmazonBillingGetPass.setEnabled(false);
        binding.buttonAmazonBillingGetPass.setOnClickListener(v -> purchasePass());

        binding.textViewAmazonBillingMoreInfo.setOnClickListener(
                v -> Utils.launchWebsite(v.getContext(), getString(R.string.url_whypay)));

        binding.progressBarAmazonBilling.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // no need to get product data every time we were hidden, so do it in onStart
        AmazonHelper.getIapManager().requestProductData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AmazonHelper.getIapManager().activate();
        AmazonHelper.getIapManager().requestUserDataAndPurchaseUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        AmazonHelper.getIapManager().deactivate();
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
        final RequestId requestId = PurchasingService.purchase(
                AmazonSku.SERIESGUIDE_SUB_YEARLY.getSku());
        Timber.d("subscribe: requestId (%s)", requestId);
    }

    private void purchasePass() {
        final RequestId requestId = PurchasingService.purchase(
                AmazonSku.SERIESGUIDE_PASS.getSku());
        Timber.d("purchasePass: requestId (%s)", requestId);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AmazonIapManager.AmazonIapMessageEvent event) {
        Toast.makeText(this, event.messageResId, Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AmazonIapManager.AmazonIapAvailabilityEvent event) {
        binding.progressBarAmazonBilling.setVisibility(View.GONE);

        // enable or disable purchase buttons based on what can be purchased
        binding.buttonAmazonBillingSubscribe
                .setEnabled(event.subscriptionAvailable && !event.userHasActivePurchase);
        binding.buttonAmazonBillingGetPass
                .setEnabled(event.passAvailable && !event.userHasActivePurchase);

        // status text
        if (!event.subscriptionAvailable && !event.passAvailable) {
            // neither purchase available, probably not signed in
            binding.textViewAmazonBillingExisting.setText(R.string.subscription_not_signed_in);
        } else {
            // subscription or pass available
            // show message if either one is active
            binding.textViewAmazonBillingExisting.setText(
                    event.userHasActivePurchase ? getString(R.string.upgrade_success) : null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(AmazonIapManager.AmazonIapProductEvent event) {
        Product product = event.product;
        // display the actual price like "1.23 C"
        String price = product.getPrice();
        if (price == null) {
            price = "--";
        }
        if (AmazonSku.SERIESGUIDE_SUB_YEARLY.getSku().equals(product.getSku())) {
            binding.textViewAmazonBillingSubPrice.setText(
                    getString(R.string.billing_price_subscribe,
                            price, getString(R.string.amazon))
            );
        } else if (AmazonSku.SERIESGUIDE_PASS.getSku().equals(product.getSku())) {
            binding.textViewAmazonBillingPricePass.setText(
                    String.format("%s\n%s", price, getString(R.string.billing_price_pass)));
        }
    }
}
