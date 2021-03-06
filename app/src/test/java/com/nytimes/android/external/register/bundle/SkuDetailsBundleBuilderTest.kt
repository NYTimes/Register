package com.nytimes.android.external.register.bundle

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.nytimes.android.external.register.APIOverrides
import com.nytimes.android.external.register.model.Config
import com.nytimes.android.external.register.model.ConfigSku
import com.nytimes.android.external.registerlib.GoogleUtil
import io.reactivex.exceptions.Exceptions
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

@RunWith(AndroidJUnit4::class)
class SkuDetailsBundleBuilderTest {

    private val apiOverrides: APIOverrides = mock()

    private val config: Config = mock()

    private lateinit var testObject: SkuDetailsBundleBuilder

    @Before
    fun setUp() {
        val packageName = "com.my.pkg"
        val description = "some description"
        val title = "caps for sale"
        val price = "1.98"

        testObject = SkuDetailsBundleBuilder(apiOverrides, config)

        whenever(config.skus).thenReturn(mutableMapOf<String, ConfigSku>().apply {
            put(SKU1, ConfigSku(TYPE, price, title, description, packageName))
            put(SKU2, ConfigSku(TYPE, price, title, description, packageName))
        })
    }

    @Test
    fun testBundleOK() {
        whenever(apiOverrides.getSkuDetailsResponse).thenReturn(GoogleUtil.RESULT_OK)

        val bundle = testObject.newBuilder()
                .skus(listOf(SKU1, SKU2), TYPE)
                .build()

        assertThat(bundle.getInt(GoogleUtil.RESPONSE_CODE))
                .isEqualTo(GoogleUtil.RESULT_OK)
        val detailsList = bundle.getStringArrayList(GoogleUtil.DETAILS_LIST)
        try {
            JSONAssert.assertEquals(
                    "{\"type\":\"subs\",\"productId\":\"sku1\",\"price\":\"$1.98\"," +
                            "\"description\":\"some description\",\"title\":\"caps for sale\"," +
                            "\"price_amount_micros\":1980000,\"price_currency_code\":\"USD\"}",
                    detailsList[0],
                    JSONCompareMode.LENIENT)
            JSONAssert.assertEquals(
                    "{\"type\":\"subs\",\"productId\":\"sku2\",\"price\":\"$1.98\"," +
                            "\"description\":\"some description\",\"title\":\"caps for sale\"," +
                            "\"price_amount_micros\":1980000,\"price_currency_code\":\"USD\"}",
                    detailsList[1],
                    JSONCompareMode.LENIENT)
        } catch (e: JSONException) {
            throw Exceptions.propagate(e)
        }

    }

    @Test
    fun testBundleNotOK() {
        whenever(apiOverrides.getSkuDetailsResponse).thenReturn(GoogleUtil.RESULT_ERROR)

        val bundle = testObject.newBuilder()
                .skus(listOf(SKU1, SKU2), TYPE)
                .build()

        assertThat(bundle.getInt(GoogleUtil.RESPONSE_CODE))
                .isEqualTo(GoogleUtil.RESULT_ERROR)

        assertThat(bundle.getStringArrayList(GoogleUtil.DETAILS_LIST))
                .isNull()
    }

    @Test
    fun testRawResponseCode() {
        testObject.rawResponseCode()
        verify<APIOverrides>(apiOverrides).getSkuDetailsResponse
    }

    companion object {
        private const val SKU1 = "sku1"
        private const val SKU2 = "sku2"
        private const val TYPE = GoogleUtil.BILLING_TYPE_SUBSCRIPTION
    }
}

