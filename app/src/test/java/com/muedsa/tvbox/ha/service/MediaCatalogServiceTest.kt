package com.muedsa.tvbox.ha.service

import com.muedsa.tvbox.ha.TestOkHttpClient
import com.muedsa.tvbox.ha.checkMediaCard
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaCatalogServiceTest {

    private val service = MediaCatalogService(okHttpClient = TestOkHttpClient)

    @Test
    fun getConfig_test() = runTest {
        val config = service.getConfig()
        check(config.pageSize > 0)
        check(config.catalogOptions.isNotEmpty())
        check(config.catalogOptions.size == config.catalogOptions.distinctBy { it.value }.size)
        for (option in config.catalogOptions) {
            check(option.items.isNotEmpty())
            check(option.items.size == option.items.distinctBy { it.value }.size)
        }
        check(config.cardWidth > 0)
        check(config.cardHeight > 0)
    }

    @Test
    fun catalog_test() = runTest {
        val config = service.getConfig()
        val selectedOptions = listOf(
            config.catalogOptions[0].copy(items = listOf(config.catalogOptions[0].items[0])),
            config.catalogOptions[1].copy(items = listOf(config.catalogOptions[1].items[5]))
        )
        val pagingResult = service.catalog(
            options = selectedOptions,
            loadKey = config.initKey,
            loadSize = config.pageSize
        )
        check(pagingResult.list.isNotEmpty())
        pagingResult.list.forEach {
            checkMediaCard(it, config.cardType)
        }
    }

}