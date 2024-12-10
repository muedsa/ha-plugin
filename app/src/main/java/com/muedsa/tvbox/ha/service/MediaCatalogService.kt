package com.muedsa.tvbox.ha.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCatalogConfig
import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.api.data.PagingResult
import com.muedsa.tvbox.api.service.IMediaCatalogService
import com.muedsa.tvbox.ha.HaConsts
import com.muedsa.tvbox.ha.helper.TagOptionPrefix
import com.muedsa.tvbox.ha.helper.parsePagedVideosFromSearchPage
import com.muedsa.tvbox.ha.helper.parseSearchOptionsFromSearchPage
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class MediaCatalogService(
    private val okHttpClient: OkHttpClient,
) : IMediaCatalogService {

    override suspend fun getConfig(): MediaCatalogConfig {
        val body = HaConsts.SEARCH_URL.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        val options = parseSearchOptionsFromSearchPage(body)
        return MediaCatalogConfig(
            initKey = "1",
            pageSize = 60,
            cardWidth = HaConsts.VERTICAL_CARD_WIDTH,
            cardHeight = HaConsts.VERTICAL_CARD_HEIGHT,
            catalogOptions = options
        )
    }

    override suspend fun catalog(
        options: List<MediaCatalogOption>,
        loadKey: String,
        loadSize: Int
    ): PagingResult<MediaCard> {
        val genre = options.find { option -> option.value == "genre" }?.items[0]?.value
        val tags = options.filter { option -> option.value.startsWith(TagOptionPrefix) }
            .flatMap { it.items }.map { it.value }
        val body = Request.Builder().url(
            HaConsts.SEARCH_URL.toHttpUrl().newBuilder()
                .setQueryParameter("query", "")
                .setQueryParameter("page", loadKey)
                .apply {
                    if (genre != null) {
                        setQueryParameter("genre", genre)
                    }
                    tags.forEach { addQueryParameter("tag", it) }
                }
                .build()
        ).feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        return parsePagedVideosFromSearchPage(body).first
    }
}