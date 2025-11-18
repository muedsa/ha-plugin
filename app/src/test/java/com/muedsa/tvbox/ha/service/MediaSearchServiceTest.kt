package com.muedsa.tvbox.ha.service

import com.muedsa.tvbox.ha.TestOkHttpClient
import com.muedsa.tvbox.ha.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaSearchServiceTest {

    private val service = MediaSearchService(okHttpClient = TestOkHttpClient)

    @Test
    fun searchMedias_test() = runTest {
        val row = service.searchMedias("")
        checkMediaCardRow(row = row)
    }
}