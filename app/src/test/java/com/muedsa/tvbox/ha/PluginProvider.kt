package com.muedsa.tvbox.ha

import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.tool.IPv6Checker
import com.muedsa.tvbox.tool.PluginCookieJar
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.createOkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy

val TestPluginPrefStore by lazy {
    FakePluginPrefStore()
}

val TestPlugin by lazy {
    HaPlugin(
        tvBoxContext = TvBoxContext(
            screenWidth = 1920,
            screenHeight = 1080,
            debug = true,
            store = TestPluginPrefStore,
            iPv6Status = IPv6Checker.checkIPv6Support()
        )
    )
}

val TestOkHttpClient by lazy {
    createOkHttpClient(
        debug = true,
        cookieJar = PluginCookieJar(saver = SharedCookieSaver(store = TestPluginPrefStore)),
        onlyIpv4 = IPv6Checker.checkIPv6Support() != IPv6Checker.IPv6Status.SUPPORTED,
    ) {
        proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 23333)))
    }
}