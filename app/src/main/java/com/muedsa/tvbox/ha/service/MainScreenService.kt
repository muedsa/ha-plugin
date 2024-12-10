package com.muedsa.tvbox.ha.service

import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.ha.HaConsts
import com.muedsa.tvbox.ha.helper.parseHomePageBody
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MainScreenService(
    private val okHttpClient: OkHttpClient,
) : IMainScreenService {

    override suspend fun getRowsData(): List<MediaCardRow> {
        val body = HaConsts.HOME_URL.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        return parseHomePageBody(body).filter { row -> row.list.isNotEmpty() }
    }
}