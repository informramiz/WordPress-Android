package org.wordpress.android.viewmodel.giphy.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.tenor.android.core.constant.MediaFilter
import com.tenor.android.core.network.IApiClient
import com.tenor.android.core.response.WeakRefCallback
import com.tenor.android.core.response.impl.GifsResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.TestApplication
import org.wordpress.android.viewmodel.giphy.provider.GifProvider.GifRequestFailedException
import org.wordpress.android.viewmodel.giphy.provider.TenorProviderTestFixtures.expectedGifMediaViewModelCollection
import org.wordpress.android.viewmodel.giphy.provider.TenorProviderTestFixtures.mockedTenorResult
import retrofit2.Call

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class TenorProviderTest {
    @Mock lateinit var apiClient: IApiClient

    @Mock lateinit var gifSearchCall: Call<GifsResponse>

    @Mock lateinit var gifResponse: GifsResponse

    @Captor lateinit var callbackCaptor: ArgumentCaptor<WeakRefCallback<Context, GifsResponse>>

    private lateinit var tenorProviderUnderTest: TenorProvider

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(apiClient.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(gifSearchCall)

        val gifResults = mockedTenorResult
        whenever(gifResponse.results).thenReturn(gifResults)

        tenorProviderUnderTest = TenorProvider(ApplicationProvider.getApplicationContext(), apiClient)
    }

    @Test
    fun `search call should invoke onSuccess with expected GIF list and nextPosition for valid query`() {
        var onSuccessWasCalled = false
        whenever(gifResponse.next).thenReturn("0")

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { actualViewModelCollection, _ ->
                    onSuccessWasCalled = true
                    assertEquals(expectedGifMediaViewModelCollection, actualViewModelCollection)
                },
                onFailure = {
                    fail("Failure handler should not be called")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), gifResponse)
        assertTrue("onSuccess should be called", onSuccessWasCalled)
    }

    @Test
    fun `search call should invoke onSuccess with expected nextPosition for a valid query`() {
        var onSuccessWasCalled = false
        whenever(gifResponse.next).thenReturn("0")
        val expectedNextPosition = 0

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { _, actualNextPosition ->
                    onSuccessWasCalled = true
                    assertEquals(expectedNextPosition, actualNextPosition)
                },
                onFailure = {
                    fail("Failure handler should not be called")
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), gifResponse)
        assertTrue("onSuccess should be called", onSuccessWasCalled)
    }

    @Test
    fun `search call should invoke onFailure when callback returns failure`() {
        var onFailureWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { _, _ ->
                    fail("Success handler should not be called")
                },
                onFailure = {
                    onFailureWasCalled = true
                    assertTrue(it is GifRequestFailedException)
                    assertEquals("Expected message", it.message)
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.failure(ApplicationProvider.getApplicationContext(), RuntimeException("Expected message"))
        assertTrue("onFailure should be called", onFailureWasCalled)
    }

    @Test
    fun `search call should invoke onFailure when null GifResponse is returned`() {
        var onFailureWasCalled = false

        tenorProviderUnderTest.search("test",
                0,
                onSuccess = { _, _ ->
                    fail("Success handler should not be called")
                },
                onFailure = {
                    onFailureWasCalled = true
                    assertTrue(it is GifRequestFailedException)
                    assertEquals("No media matching your search", it.message)
                })

        verify(gifSearchCall, times(1)).enqueue(callbackCaptor.capture())
        val capturedCallback = callbackCaptor.value
        capturedCallback.success(ApplicationProvider.getApplicationContext(), null)
        assertTrue("onFailure should be called", onFailureWasCalled)
    }

    @Test
    fun `search call must use BASIC as MediaFilter`() {
        val argument = ArgumentCaptor.forClass(String::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                any(),
                any(),
                argument.capture(),
                any()
        )

        assertEquals(MediaFilter.BASIC, argument.value)
    }

    @Test
    fun `search call without loadSize should use default maximum value`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        assertEquals(50, argument.value)
    }

    @Test
    fun `search call with loadSize lower than 50 should be used`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                20,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        assertEquals(20, argument.value)
    }

    @Test
    fun `search call with loadSize higher than 50 should be reduced back to default maximum value`() {
        val argument = ArgumentCaptor.forClass(Int::class.java)

        tenorProviderUnderTest.search(
                "test",
                0,
                1500,
                onSuccess = { _, _ -> },
                onFailure = {})

        verify(apiClient).search(
                any(),
                any(),
                argument.capture(),
                any(),
                any(),
                any()
        )

        assertEquals(50, argument.value)
    }
}
