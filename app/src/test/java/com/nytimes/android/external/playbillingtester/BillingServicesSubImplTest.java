package com.nytimes.android.external.playbillingtester;

import android.os.Bundle;
import android.os.RemoteException;

import com.google.common.collect.ImmutableList;
import com.nytimes.android.external.playbillingtester.bundle.BuyIntentBundleBuilder;
import com.nytimes.android.external.playbillingtester.bundle.ConsumePurchaseResponse;
import com.nytimes.android.external.playbillingtester.bundle.PurchasesBundleBuilder;
import com.nytimes.android.external.playbillingtester.bundle.SkuDetailsBundleBuilder;
import com.nytimes.android.external.playbillingtester.model.Config;
import com.nytimes.android.external.playbillingtesterlib.GoogleUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.nytimes.android.external.playbillingtester.APIOverrides.RESULT_DEFAULT;
import static com.nytimes.android.external.playbillingtester.di.GsonFactory.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class BillingServicesSubImplTest {

    private static final int API_VERSION = 3;
    private static final String PACKAGE_NAME = "com.my.package";
    private static final String DEVELOPER_PAYLOAD = "devPayload";
    private static final String SKU = "sku";

    @Mock
    private APIOverrides apiOverrides;

    @Mock
    private Config config;

    @Mock
    private BuyIntentBundleBuilder buyIntentBundleBuilder;

    @Mock
    private SkuDetailsBundleBuilder skuDetailsBundleBuilder;

    @Mock
    private PurchasesBundleBuilder purchasesBundleBuilder;

    private ConsumePurchaseResponse consumePurchaseResponse;

    private BillingServiceStubImpl testObject;

    private final String type = GoogleUtil.BILLING_TYPE_IAP;
    private final Bundle expected = new Bundle();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(skuDetailsBundleBuilder.newBuilder()).thenReturn(skuDetailsBundleBuilder);
        when(skuDetailsBundleBuilder.skus(anyListOf(String.class), anyString())).thenReturn(skuDetailsBundleBuilder);
        when(skuDetailsBundleBuilder.build()).thenReturn(expected);
        when(buyIntentBundleBuilder.newBuilder()).thenReturn(buyIntentBundleBuilder);
        when(buyIntentBundleBuilder.developerPayload(anyString())).thenReturn(buyIntentBundleBuilder);
        when(buyIntentBundleBuilder.packageName(anyString())).thenReturn(buyIntentBundleBuilder);
        when(buyIntentBundleBuilder.sku(anyString())).thenReturn(buyIntentBundleBuilder);
        when(buyIntentBundleBuilder.type(anyString())).thenReturn(buyIntentBundleBuilder);
        when(buyIntentBundleBuilder.build()).thenReturn(expected);
        when(purchasesBundleBuilder.newBuilder()).thenReturn(purchasesBundleBuilder);
        when(purchasesBundleBuilder.type(anyString())).thenReturn(purchasesBundleBuilder);
        when(purchasesBundleBuilder.continuationToken(anyString())).thenReturn(purchasesBundleBuilder);
        when(purchasesBundleBuilder.build()).thenReturn(expected);
        consumePurchaseResponse = new ConsumePurchaseResponse(apiOverridesAndPurchases);
        testObject = new BillingServiceStubImpl(apiOverridesAndPurchases, create(), config,
                buyIntentBundleBuilder, skuDetailsBundleBuilder, purchasesBundleBuilder, consumePurchaseResponse);
    }

    @Test
    public void testIsBillingSupportedDefault() {
        int expected = GoogleUtil.RESULT_OK;
        when(apiOverrides.getIsBillingSupportedResponse()).thenReturn(RESULT_DEFAULT);
        int actual = testObject.isBillingSupported(API_VERSION, PACKAGE_NAME, type);
        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    public void testIsBillingSupportedNonDefault() {
        int expected = GoogleUtil.RESULT_BILLING_UNAVAILABLE;
        when(apiOverrides.getIsBillingSupportedResponse()).thenReturn(expected);
        int actual = testObject.isBillingSupported(API_VERSION, PACKAGE_NAME, type);
        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    public void testGetSkuDetails() {
        Bundle skusBundle = new Bundle();
        skusBundle.putStringArrayList(GoogleUtil.ITEM_ID_LIST, new ArrayList<>());
        Bundle actual = testObject.getSkuDetails(API_VERSION, PACKAGE_NAME, type, skusBundle);
        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    public void testGetBuyIntent() {
        Bundle actual = testObject.getBuyIntent(API_VERSION, SKU, PACKAGE_NAME, type, DEVELOPER_PAYLOAD);
        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    public void testGetPurchases() {
        String continuationToken = "conti";
        Bundle actual = testObject.getPurchases(API_VERSION, PACKAGE_NAME, type, continuationToken);
        assertThat(actual)
                .isEqualTo(expected);
    }

    @Test
    public void testConsumePurchase() {
        String purchaseToken = "token";
        assertThat(testObject.consumePurchase(API_VERSION, PACKAGE_NAME, purchaseToken))
                .isEqualTo(GoogleUtil.RESULT_ITEM_NOT_OWNED);
    }

    @Test
    public void testConsumePurchaseIAP() {
        final List<String> purchases = ImmutableList.of("purchase1", "purchase2");
        final List<String> subscriptions = ImmutableList.of("subscription1", "subscription2");
        Bundle getPurchasesBundle = new Bundle();
        getPurchasesBundle.putStringArrayList(GoogleUtil.INAPP_PURCHASE_DATA_LIST, new ArrayList<>(purchases));
        when(apiOverridesAndPurchases.getInAppPurchaseDataAsArrayList(GoogleUtil.BILLING_TYPE_IAP)).thenReturn(purchases);
        when(apiOverridesAndPurchases.getInAppPurchaseDataAsArrayList(GoogleUtil.BILLING_TYPE_SUBSCRIPTION)).thenReturn(subscriptions);
        when(purchasesBundleBuilder.build()).thenReturn(getPurchasesBundle);

        Bundle stored = testObject.getPurchases(API_VERSION, PACKAGE_NAME, type, "token");
        assertThat(stored.getInt(GoogleUtil.RESPONSE_CODE))
                .isEqualTo(GoogleUtil.RESULT_OK);
        assertThat(stored.getStringArrayList(GoogleUtil.INAPP_PURCHASE_DATA_LIST))
                .isEqualTo(purchases);
        assertThat(stored).isEqualTo(getPurchasesBundle);

        String purchaseToken = purchases.get(0);
        int result = testObject.consumePurchase(API_VERSION, PACKAGE_NAME, purchaseToken);
        assertThat(result).isEqualTo(GoogleUtil.RESULT_OK);
    }

    @Test
    public void testConsumePurchaseSubscription() {
        final List<String> purchases = ImmutableList.of("purchase1", "purchase2");
        final List<String> subscriptions = ImmutableList.of("subscription1", "subscription2");
        Bundle getPurchasesBundle = new Bundle();
        getPurchasesBundle.putStringArrayList(GoogleUtil.INAPP_PURCHASE_DATA_LIST, new ArrayList<>(purchases));
        when(apiOverridesAndPurchases.getInAppPurchaseDataAsArrayList(GoogleUtil.BILLING_TYPE_IAP)).thenReturn(purchases);
        when(apiOverridesAndPurchases.getInAppPurchaseDataAsArrayList(GoogleUtil.BILLING_TYPE_SUBSCRIPTION)).thenReturn(subscriptions);
        when(purchasesBundleBuilder.build()).thenReturn(getPurchasesBundle);

        Bundle stored = testObject.getPurchases(API_VERSION, PACKAGE_NAME, type, "token");
        assertThat(stored.getInt(GoogleUtil.RESPONSE_CODE))
                .isEqualTo(GoogleUtil.RESULT_OK);
        assertThat(stored.getStringArrayList(GoogleUtil.INAPP_PURCHASE_DATA_LIST))
                .isEqualTo(purchases);
        assertThat(stored).isEqualTo(getPurchasesBundle);

        String purchaseToken = subscriptions.get(0);

        int result = testObject.consumePurchase(API_VERSION, PACKAGE_NAME, purchaseToken);
        assertThat(result).isEqualTo(GoogleUtil.RESULT_ERROR);
    }

    @Test
    public void testConsumePurchaseNotOwned() {
        final List<String> purchases = ImmutableList.of("purchase1", "purchase2");
        final List<String> subscriptions = ImmutableList.of("subscription1", "subscription2");
        Bundle getPurchasesBundle = new Bundle();
        getPurchasesBundle.putStringArrayList(GoogleUtil.INAPP_PURCHASE_DATA_LIST, new ArrayList<>(purchases));
        when(apiOverridesAndPurchases.getInAppPurchaseDataAsArrayList(GoogleUtil.BILLING_TYPE_IAP)).thenReturn(purchases);
        when(apiOverridesAndPurchases.getInAppPurchaseDataAsArrayList(GoogleUtil.BILLING_TYPE_SUBSCRIPTION)).thenReturn(subscriptions);
        when(purchasesBundleBuilder.build()).thenReturn(getPurchasesBundle);

        Bundle stored = testObject.getPurchases(API_VERSION, PACKAGE_NAME, type, "token");
        assertThat(stored.getInt(GoogleUtil.RESPONSE_CODE))
                .isEqualTo(GoogleUtil.RESULT_OK);
        assertThat(stored.getStringArrayList(GoogleUtil.INAPP_PURCHASE_DATA_LIST))
                .isEqualTo(purchases);
        assertThat(stored).isEqualTo(getPurchasesBundle);

        String purchaseToken = "Not owned product";

        int result = testObject.consumePurchase(API_VERSION, PACKAGE_NAME, purchaseToken);
        assertThat(result).isEqualTo(GoogleUtil.RESULT_ITEM_NOT_OWNED);
    }

    @Test
    public void testStub() throws RemoteException {
        assertThat(testObject.stub(API_VERSION, PACKAGE_NAME, type))
                .isEqualTo(0);
    }

    @Test
    public void testGutBuyIntentToReplaceSkus() throws RemoteException {
        String sku2 = "sku2";
        assertThat(testObject.getBuyIntentToReplaceSkus(API_VERSION, PACKAGE_NAME,
                ImmutableList.of(SKU), sku2, type, DEVELOPER_PAYLOAD))
                .isNull();
    }

    @Test
    public void testAsBinder() {
        assertThat(testObject.asBinder())
                .isEqualTo(testObject);
    }
}
