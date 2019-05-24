package com.nytimes.android.external.register

import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import com.google.common.base.Optional
import com.google.common.collect.ImmutableSet
import com.nytimes.android.external.register.APIOverrides.RESULT_DEFAULT
import com.nytimes.android.external.register.BuyActivity.RECEIPT_FMT
import com.nytimes.android.external.register.model.Config
import com.nytimes.android.external.registerlib.GoogleUtil
import com.nytimes.android.external.registerlib.InAppPurchaseData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowLooper
import java.util.*

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(constants = BuildConfig::class, sdk = [21])
class MainActivityTest {

    private lateinit var testObject: MainActivity
    private lateinit var controller: ActivityController<*>
    private lateinit var shadowMain: ShadowActivity

    @Mock
    private lateinit var purchases: Purchases
    @Mock
    private lateinit var mockApiDelegate: APIOverridesDelegate
    @Mock
    private lateinit var config: Config

    private val inAppPurchaseData1 = InAppPurchaseData.Builder()
            .orderId(java.lang.Long.toString(CURRENT_TIME_MS))
            .packageName(packageName)
            .productId(SKU1)
            .purchaseTime(java.lang.Long.toString(CURRENT_TIME_MS))
            .developerPayload(DEVELOPER_PAYLOAD)
            .purchaseToken(String.format(Locale.getDefault(), RECEIPT_FMT, USER1, CURRENT_TIME_MS))
            .build()
    private val inAppPurchaseData2 = InAppPurchaseData.Builder()
            .orderId(java.lang.Long.toString(CURRENT_TIME_MS))
            .packageName(packageName)
            .productId(SKU2)
            .purchaseTime(java.lang.Long.toString(CURRENT_TIME_MS))
            .developerPayload(DEVELOPER_PAYLOAD)
            .purchaseToken(String.format(Locale.getDefault(), RECEIPT_FMT, USER2, CURRENT_TIME_MS))
            .build()

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        controller = Robolectric.buildActivity(MainActivityTest.TestMainActivity::class.java).create()
        testObject = controller.get() as MainActivity
        testObject.apiDelegate = mockApiDelegate
        testObject.purchases = purchases
        testObject.config = Optional.of(config)
        shadowMain = Shadow.extract<ShadowActivity>(testObject)
    }

    @Test
    fun updatePurchases_whenHas_showsItems() {
        // Setup
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_IAP))
                .thenReturn(ImmutableSet.of(inAppPurchaseData1))  // init
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_SUBSCRIPTION))
                .thenReturn(ImmutableSet.of(inAppPurchaseData2))  // init

        val list = testObject.findViewById<View>(R.id.list) as RecyclerView
        val emptyView = testObject.findViewById<View>(R.id.empty_view)

        // Verify empty
        controller.start().postResume()
        assertThat(list.adapter.itemCount).isEqualTo(2)
        assertThat(emptyView.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun updatePurchases_whenNone_showsEmpty() {
        // Setup
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_IAP))
                .thenReturn(ImmutableSet.of())  // init
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_SUBSCRIPTION))
                .thenReturn(ImmutableSet.of())   // init

        val list = testObject.findViewById<View>(R.id.list) as RecyclerView
        val emptyView = testObject.findViewById<View>(R.id.empty_view)

        // Verify empty
        controller.start().postResume()
        assertThat(list.adapter.itemCount).isEqualTo(0)
        assertThat(emptyView.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun onMenuClick_whenDeleteAll_showsNoItems() {
        // Setup
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_IAP))
                .thenReturn(ImmutableSet.of(inAppPurchaseData1)) // Initial
                .thenReturn(ImmutableSet.of()) // After purge

        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(R.id.menu_action_delete_all)

        val list = testObject.findViewById<View>(R.id.list) as RecyclerView

        // Start and make sure we have 1 IAP
        controller.start().postResume()
        assertThat(list.adapter.itemCount).isEqualTo(1)

        // Purge and make sure we are empty
        testObject.onOptionsItemSelected(item)
        verify(purchases).purgePurchases()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(list.adapter.itemCount).isEqualTo(0)
    }

    @Test
    fun onMenuClick_whenRefresh_refreshContent() {
        // Setup
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_IAP))
                .thenReturn(ImmutableSet.of())   // init
                .thenReturn(ImmutableSet.of(inAppPurchaseData1))   // after refresh
        `when`(purchases.getInAppPurchaseData(GoogleUtil.BILLING_TYPE_SUBSCRIPTION))
                .thenReturn(ImmutableSet.of())   // init
                .thenReturn(ImmutableSet.of(inAppPurchaseData2))   // after refresh

        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(R.id.menu_action_refresh)

        val list = testObject.findViewById<View>(R.id.list) as RecyclerView

        // Verify empty
        controller.start().postResume()
        assertThat(list.adapter.itemCount).isEqualTo(0)

        //Refresh
        testObject.onOptionsItemSelected(item)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        assertThat(list.adapter.itemCount).isEqualTo(2)
    }

    @Test
    fun onMenuClick_whenSettings_startsSettings() {
        // Setup
        val item = mock(MenuItem::class.java)
        `when`(item.itemId).thenReturn(R.id.menu_action_settings)

        controller.start()
        testObject.onOptionsItemSelected(item)

        // Verify SettingsActivity started
        val startedIntent = shadowMain.nextStartedActivity
        val shadowIntent = shadowOf(startedIntent)
        assertThat(shadowIntent.intentClass).isEqualTo(SettingsActivity::class.java)
    }

    @Test
    fun onCreate_whenDefaultValues_showsDefaultValues() {
        `when`(mockApiDelegate.getApiOverridesValue(R.id.isBillingSupported)).thenReturn(RESULT_DEFAULT)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getBuyIntent)).thenReturn(RESULT_DEFAULT)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.buy)).thenReturn(RESULT_DEFAULT)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getPurchases)).thenReturn(RESULT_DEFAULT)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getSkuDetails)).thenReturn(RESULT_DEFAULT)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getBuyIntentToReplaceSkus)).thenReturn(RESULT_DEFAULT)

        controller.postCreate(null).start()

        assertThat(getSpinnerValue(R.id.isBillingSupported)).isEqualTo(RESULT_DEFAULT.toLong())
        assertThat(getSpinnerValue(R.id.getBuyIntent)).isEqualTo(RESULT_DEFAULT.toLong())
        assertThat(getSpinnerValue(R.id.buy)).isEqualTo(RESULT_DEFAULT.toLong())
        assertThat(getSpinnerValue(R.id.getPurchases)).isEqualTo(RESULT_DEFAULT.toLong())
        assertThat(getSpinnerValue(R.id.getSkuDetails)).isEqualTo(RESULT_DEFAULT.toLong())
        assertThat(getSpinnerValue(R.id.getBuyIntentToReplaceSkus)).isEqualTo(RESULT_DEFAULT.toLong())
    }

    @Test
    fun onCreate_whenUniqueValues_showsUniqueValues() {
        `when`(mockApiDelegate.getApiOverridesValue(R.id.isBillingSupported))
                .thenReturn(GoogleUtil.RESULT_BILLING_UNAVAILABLE)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getBuyIntent)).thenReturn(GoogleUtil.RESULT_OK)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.buy)).thenReturn(GoogleUtil.RESULT_ITEM_UNAVAILABLE)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getPurchases)).thenReturn(GoogleUtil.RESULT_DEVELOPER_ERROR)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getSkuDetails)).thenReturn(GoogleUtil.RESULT_ERROR)
        `when`(mockApiDelegate.getApiOverridesValue(R.id.getBuyIntentToReplaceSkus))
                .thenReturn(GoogleUtil.RESULT_ITEM_NOT_OWNED)

        controller.postCreate(null).start()

        assertThat(getSpinnerValue(R.id.isBillingSupported)).isEqualTo(GoogleUtil.RESULT_BILLING_UNAVAILABLE.toLong())
        assertThat(getSpinnerValue(R.id.getBuyIntent)).isEqualTo(GoogleUtil.RESULT_OK.toLong())
        assertThat(getSpinnerValue(R.id.buy)).isEqualTo(GoogleUtil.RESULT_ITEM_UNAVAILABLE.toLong())
        assertThat(getSpinnerValue(R.id.getPurchases)).isEqualTo(GoogleUtil.RESULT_DEVELOPER_ERROR.toLong())
        assertThat(getSpinnerValue(R.id.getSkuDetails)).isEqualTo(GoogleUtil.RESULT_ERROR.toLong())
        assertThat(getSpinnerValue(R.id.getBuyIntentToReplaceSkus)).isEqualTo(GoogleUtil.RESULT_ITEM_NOT_OWNED.toLong())
    }

    @Test
    fun onItemSelected_whenChanged_setsApiOverrides() {
        controller.postCreate(null).start()

        val isBillingSupportedSpinner = getInternalSpinner(R.id.isBillingSupported)
        val getBuyIntentSpinner = getInternalSpinner(R.id.getBuyIntent)
        val buySpinner = getInternalSpinner(R.id.buy)
        val getPurchasesSpinner = getInternalSpinner(R.id.getPurchases)
        val getSkuDetailsSpinner = getInternalSpinner(R.id.getSkuDetails)
        val getBuyIntentReplaceSkusSpinner = getInternalSpinner(R.id.getBuyIntentToReplaceSkus)

        assertThat(isBillingSupportedSpinner.tag).isNull()
        assertThat(getBuyIntentSpinner.tag).isNull()
        assertThat(buySpinner.tag).isNull()
        assertThat(getPurchasesSpinner.tag).isNull()
        assertThat(getSkuDetailsSpinner.tag).isNull()
        assertThat(getBuyIntentReplaceSkusSpinner.tag).isNull()

        val parentView = mock(AdapterView::class.java)
        val view = mock(View::class.java)

        val argumentCaptor = ArgumentCaptor.forClass(ConfigResponse::class.java)

        isBillingSupportedSpinner.onItemSelectedListener.onItemSelected(parentView, view, 0, 0L)
        verify<APIOverridesDelegate>(mockApiDelegate).setApiOverridesValue(eq(R.id.isBillingSupported), argumentCaptor.capture())
        assertThat(argumentCaptor.value.responseCode()).isEqualTo(RESULT_DEFAULT)

        getBuyIntentSpinner.onItemSelectedListener.onItemSelected(parentView, view, 1, 0L)
        verify<APIOverridesDelegate>(mockApiDelegate).setApiOverridesValue(eq(R.id.getBuyIntent), argumentCaptor.capture())
        assertThat(argumentCaptor.value.responseCode()).isEqualTo(GoogleUtil.RESULT_OK)

        buySpinner.onItemSelectedListener.onItemSelected(parentView, view, 2, 0L)
        verify<APIOverridesDelegate>(mockApiDelegate).setApiOverridesValue(eq(R.id.buy), argumentCaptor.capture())
        assertThat(argumentCaptor.value.responseCode()).isEqualTo(GoogleUtil.RESULT_ITEM_UNAVAILABLE)

        getPurchasesSpinner.onItemSelectedListener.onItemSelected(parentView, view, 3, 0L)
        verify<APIOverridesDelegate>(mockApiDelegate).setApiOverridesValue(eq(R.id.getPurchases), argumentCaptor.capture())
        assertThat(argumentCaptor.value.responseCode()).isEqualTo(GoogleUtil.RESULT_ERROR)

        getSkuDetailsSpinner.onItemSelectedListener.onItemSelected(parentView, view, 4, 0L)
        verify<APIOverridesDelegate>(mockApiDelegate).setApiOverridesValue(eq(R.id.getSkuDetails), argumentCaptor.capture())
        assertThat(argumentCaptor.value.responseCode()).isEqualTo(GoogleUtil.RESULT_ERROR)

        getBuyIntentReplaceSkusSpinner.onItemSelectedListener.onItemSelected(parentView, view, 5, 0L)
        verify<APIOverridesDelegate>(mockApiDelegate).setApiOverridesValue(eq(R.id.getBuyIntentToReplaceSkus), argumentCaptor.capture())
        assertThat(argumentCaptor.value.responseCode()).isEqualTo(GoogleUtil.RESULT_ITEM_ALREADY_OWNED)
    }

    private fun getSpinnerValue(parentId: Int): Long {
        return getInternalSpinner(parentId).selectedItemId
    }

    private fun getInternalSpinner(parentId: Int): AdapterView<Adapter> {
        return testObject.findViewById<View>(parentId)
                .findViewById<View>(R.id.config_spinner) as AdapterView<Adapter>
    }

    internal class TestMainActivity : MainActivity() {
        override fun inject() {
            //intentionally blank
        }
    }

    companion object {
        private const val DEVELOPER_PAYLOAD = "devPayload"
        private const val packageName = "com.my.pkg"
        private const val USER1 = "myfun@user.com"
        private const val USER2 = "myfun2@user.com"
        private const val CURRENT_TIME_MS = 1234567L
        private const val SKU1 = "sku1"
        private const val SKU2 = "sku2"
    }
}