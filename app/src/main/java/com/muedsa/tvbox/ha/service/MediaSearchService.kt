package com.muedsa.tvbox.ha.service

import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.ha.HaConsts
import com.muedsa.tvbox.ha.helper.parsePagedVideosFromSearchPage
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class MediaSearchService(
    private val okHttpClient: OkHttpClient,
) : IMediaSearchService {
    override suspend fun searchMedias(query: String): MediaCardRow {
        val body = Request.Builder().url(
            HaConsts.SEARCH_URL.toHttpUrl().newBuilder()
                .setQueryParameter("query", query)
                .setQueryParameter("page", "1")
                .build()
        ).feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        val result = parsePagedVideosFromSearchPage(body)
        return MediaCardRow(
            title = "search list",
            cardWidth = if (result.second) HaConsts.HORIZONTAL_CARD_WIDTH else HaConsts.VERTICAL_CARD_WIDTH,
            cardHeight = if (result.second) HaConsts.HORIZONTAL_CARD_HEIGHT else HaConsts.VERTICAL_CARD_HEIGHT,
            list = result.first.list
        )
    }
}