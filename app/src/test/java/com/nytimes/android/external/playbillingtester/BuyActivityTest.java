package com.nytimes.android.external.playbillingtester;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.nytimes.android.external.playbillingtester.bundle.BuyIntentBundleBuilder;
import com.nytimes.android.external.playbillingtester.bundle.BuyIntentToReplaceSkusBundleBuilder;
import com.nytimes.android.external.playbillingtester.di.GsonFactory;
import com.nytimes.android.external.playbillingtester.model.Config;
import com.nytimes.android.external.playbillingtester.model.ConfigSku;
import com.nytimes.android.external.playbillingtester.model.ImmutableConfigSku;
import com.nytimes.android.external.playbillingtesterlib.GoogleUtil;
import com.nytimes.android.external.playbillingtesterlib.InAppPurchaseData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.Locale;

import edu.emory.mathcs.backport.java.util.Collections;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.nytimes.android.external.playbillingtester.BuyActivity.ERROR_FMT;
import static com.nytimes.android.external.playbillingtester.BuyActivity.RECEIPT_FMT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class, sdk = 21)
public class BuyActivityTest {

    private static final String SKU = "sku1";
    private static final String RECEIPT = "myfun@user.com.playBillingTesterToken1234567";
    private static final String TYPE = GoogleUtil.BILLING_TYPE_SUBSCRIPTION;
    private static final String CONTINUATION_TOKEN = "";
    private static final String DEVELOPER_PAYLOAD = "devPayload";
    private static final String DESCRIPTION = "some description";
    private static final String PACKAGE_NAME = "com.my.pkg";
    private static final String PRICE = "1.98";
    private static final String TITLE = "caps for sale";
    private static final String USER = "myfun@user.com";

    private static final String NEW_SKU = "sku2";

    private static final long CURRENT_TIME_MS = 1234567L;

    private BuyActivity testObject;
    private ShadowActivity shadowActivity;
    private final ConfigSku configSku = ImmutableConfigSku.builder()
            .description(DESCRIPTION)
            .itemType(TYPE)
            .packageName(PACKAGE_NAME)
            .price(PRICE)
            .title(TITLE)
            .build();
    private ImmutableMap.Builder<String, ConfigSku> configSkuMapBuilder;
    private final InAppPurchaseData inAppPurchaseData = new InAppPurchaseData.Builder()
            .orderId(Long.toString(CURRENT_TIME_MS))
            .packageName(PACKAGE_NAME)
            .productId(SKU)
            .purchaseTime(Long.toString(CURRENT_TIME_MS))
            .developerPayload(DEVELOPER_PAYLOAD)
            .purchaseToken(String.format(Locale.getDefault(), RECEIPT_FMT, USER, CURRENT_TIME_MS))
            .build();

    private ActivityController controller;
    private Gson gson = GsonFactory.create();

    @Mock
    private APIOverrides apiOverrides;
    @Mock
    private Purchases purchases;
    @Mock
    private Purchases.PurchasesLists purchasesLists;
    @Mock
    private Config config;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initTestObject(boolean isReplace) {
        Intent intent = new Intent(RuntimeEnvironment.application, TestBuyActivity.class);

        intent.putExtra(BuyIntentBundleBuilder.EX_ITEM_TYPE, TYPE);
        intent.putExtra(BuyIntentBundleBuilder.EX_DEVELOPER_PAYLOAD, DEVELOPER_PAYLOAD);
        if (isReplace) {
            intent.putExtra(BuyIntentToReplaceSkusBundleBuilder.EX_NEW_SKU, NEW_SKU);
            intent.putStringArrayListExtra(BuyIntentToReplaceSkusBundleBuilder.EX_OLD_SKUS,
                    new ArrayList<String>(Collections.singletonList(NEW_SKU)));
        } else {
            intent.putExtra(BuyIntentBundleBuilder.EX_SKU, SKU);
        }

        controller = Robolectric.buildActivity(TestBuyActivity.class, intent).create();
        testObject = (BuyActivity) controller.get();
        testObject.apiOverrides = apiOverrides;
        testObject.purchases = purchases;
        testObject.gson = gson;
        testObject.config = config;
        shadowActivity = shadowOf(testObject);

        when(purchases.getPurchasesLists(TYPE, CONTINUATION_TOKEN)).thenReturn(purchasesLists);
        configSkuMapBuilder = ImmutableMap.builder();
    }

    @Test
    public void testBuyResponseOK() {
        initTestObject(false);

        when(apiOverrides.getBuyResponse()).thenReturn(APIOverrides.RESULT_DEFAULT);
        when(purchases.getReceiptsForSkus(ImmutableSet.of(SKU), TYPE)).thenReturn(ImmutableSet.of());
        configSkuMapBuilder.put(SKU, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());

        controller.start();

        TextView titleTextView = (TextView) testObject.findViewById(R.id.buy_title);
        TextView summaryTextView = (TextView) testObject.findViewById(R.id.buy_summary);
        TextView priceTextView = (TextView) testObject.findViewById(R.id.buy_price);
        Button buyButton = (Button) testObject.findViewById(R.id.buy_button);

        assertThat(titleTextView.getText()).isEqualTo(TITLE);
        assertThat(summaryTextView.getText()).isEqualTo(DESCRIPTION);
        assertThat(priceTextView.getText()).isEqualTo("$" + PRICE);
        assertThat(buyButton.getText()).isEqualTo(getStringResource(R.string.buy));
    }

    @Test
    public void testItemUnavailable() {
        initTestObject(false);

        String sku2 = "sku2";
        when(apiOverrides.getBuyResponse()).thenReturn(APIOverrides.RESULT_DEFAULT);
        when(purchases.getReceiptsForSkus(ImmutableSet.of(SKU), TYPE)).thenReturn(ImmutableSet.of());
        configSkuMapBuilder.put(sku2, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());

        controller.start();

        assertThat(((TextView) testObject.findViewById(R.id.buy_title)).getText())
                .isEqualTo(getStringResource(R.string.error));
        assertThat(((TextView) testObject.findViewById(R.id.buy_summary)).getText())
                .isEqualTo(getStringResource(R.string.item_not_found));
    }

    @Test
    public void testItemAlreadyOwned() {
        initTestObject(false);

        when(apiOverrides.getBuyResponse()).thenReturn(APIOverrides.RESULT_DEFAULT);
        when(purchases.getReceiptsForSkus(ImmutableSet.of(SKU), TYPE)).thenReturn(ImmutableSet.of(RECEIPT));
        configSkuMapBuilder.put(SKU, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());

        controller.start();

        assertThat(((TextView) testObject.findViewById(R.id.buy_title)).getText())
                .isEqualTo(getStringResource(R.string.error));
        assertThat(((TextView) testObject.findViewById(R.id.buy_summary)).getText())
                .isEqualTo(getStringResource(R.string.item_already_owned));
    }

    @Test
    public void testError() {
        initTestObject(false);

        when(apiOverrides.getBuyResponse()).thenReturn(GoogleUtil.RESULT_ERROR);

        controller.start();

        assertThat(((TextView) testObject.findViewById(R.id.buy_title)).getText())
                .isEqualTo(getStringResource(R.string.error));
        assertThat(((TextView) testObject.findViewById(R.id.buy_summary)).getText())
                .isEqualTo(String.format(Locale.getDefault(), ERROR_FMT,
                        getStringResource(R.string.error), GoogleUtil.RESULT_ERROR));
    }

    @Test
    public void testHandleBuy() {
        initTestObject(false);

        when(apiOverrides.getUsersResponse()).thenReturn(USER);
        configSkuMapBuilder.put(SKU, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());
        String inAppPurchaseDataStr = gson.toJson(inAppPurchaseData);
        when(purchases.addPurchase(inAppPurchaseDataStr, TYPE)).thenReturn(true);

        controller.start();
        testObject.currentTimeMillis = CURRENT_TIME_MS;
        testObject.findViewById(R.id.buy_button).callOnClick();

        assertThat(shadowActivity.getResultCode())
                .isEqualTo(RESULT_OK);
        Intent resultIntent = shadowActivity.getResultIntent();
        assertThat(resultIntent.getIntExtra(GoogleUtil.RESPONSE_CODE, APIOverrides.RESULT_DEFAULT))
                .isEqualTo(GoogleUtil.RESULT_OK);
        assertThat(resultIntent.getStringExtra(GoogleUtil.INAPP_PURCHASE_DATA))
                .isEqualTo(inAppPurchaseDataStr);
        verify(purchases).addPurchase(inAppPurchaseDataStr, TYPE);
    }

    @Test
    public void testHandleAlreadyOwnedValid() {
        when(apiOverrides.getReplaceResponse())
                .thenReturn(GoogleUtil.RESULT_ITEM_ALREADY_OWNED);
        when(purchases.getInAppPurchaseData(TYPE)).thenReturn(ImmutableSet.of(inAppPurchaseData));

        initTestObject(true);
        configSkuMapBuilder.put(NEW_SKU, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());

        controller.start();
        testObject.findViewById(R.id.buy_button).callOnClick();

        assertThat(shadowActivity.getResultCode()).isEqualTo(RESULT_OK);
        Intent resultIntent = shadowActivity.getResultIntent();
        assertThat(resultIntent.getIntExtra(GoogleUtil.RESPONSE_CODE, -1))
                .isEqualTo(GoogleUtil.RESULT_ITEM_ALREADY_OWNED);
    }

    @Test
    public void testHandleAlreadyOwnedInvalid() {
        when(apiOverrides.getReplaceResponse())
                .thenReturn(GoogleUtil.RESULT_ITEM_ALREADY_OWNED);
        when(purchases.getInAppPurchaseData(TYPE)).thenReturn(ImmutableSet.of());

        initTestObject(true);
        configSkuMapBuilder.put(SKU, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());

        controller.start();
        testObject.findViewById(R.id.buy_button).callOnClick();

        assertThat(shadowActivity.getResultCode())
                .isEqualTo(RESULT_CANCELED);
        assertThat(shadowActivity.getResultIntent())
                .isNull();
    }

    @Test
    public void onBackPressedResultsOk() {
        initTestObject(false);
        configSkuMapBuilder.put(SKU, configSku);
        when(config.skus()).thenReturn(configSkuMapBuilder.build());

        controller.start();
        testObject.onBackPressed();

        assertThat(shadowActivity.getResultCode()).isEqualTo(RESULT_OK);
    }

    private String getStringResource(int id) {
        return RuntimeEnvironment.application.getResources().getString(id);
    }

    static class TestBuyActivity extends BuyActivity {

        @Override
        protected void inject() {
            //intentionally blank
        }
    }
}
