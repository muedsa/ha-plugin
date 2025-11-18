package com.muedsa.tvbox.ha.service

import com.muedsa.tvbox.ha.TestOkHttpClient
import com.muedsa.tvbox.ha.checkMediaCardRows
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainScreenServiceTest {

    private val service = MainScreenService(okHttpClient = TestOkHttpClient)

    @Test
    fun getRowsDataTest() = runTest{
        val rows = service.getRowsData()
        checkMediaCardRows(rows = rows)
    }

}