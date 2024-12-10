package com.muedsa.tvbox.ha.helper

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.api.data.MediaCatalogOptionItem
import com.muedsa.tvbox.api.data.MediaDetail
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.PagingResult
import com.muedsa.tvbox.api.data.SavedMediaCard
import com.muedsa.tvbox.ha.HaConsts
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

val VideoSourceUrlPattern = "const source = '(https://.*?)';".toRegex()
val DigitsPattern = "\\d+".toRegex()
const val TagOptionPrefix = "tags-"

fun parseHomePageBody(body: Element): List<MediaCardRow> {
    val list = mutableListOf<MediaCardRow>()
    val rowsWrapper = body.selectFirst(Evaluator.Id("home-rows-wrapper"))!!
    var i = 0
    while (i < rowsWrapper.childrenSize()) {
        val child = rowsWrapper.child(i)
        i++
        if (child.tagName() == "a" && child.childrenSize() > 0) {
            val rowTitle = child.selectFirst("h3")
            if (rowTitle != null) {
                val videosWrapper =
                    child.nextElementSibling()?.selectFirst(".home-rows-videos-wrapper")
                if (videosWrapper != null) {
                    list.add(parseRow(rowTitle, videosWrapper))
                    i++
                }
            }
        }
    }
    return list
}

private fun parseRow(rowTitle: Element, wrapper: Element): MediaCardRow {
    val title = rowTitle.text().trim()
    val horizontal = wrapper.selectFirst(".home-rows-videos-div") == null
    val cards = if (horizontal)
        parseRowHorizontalItems(wrapper, ".search-doujin-videos")
    else
        parseRowVerticalItems(wrapper)
    return MediaCardRow(
        title = title,
        list = cards,
        cardWidth = if (horizontal) HaConsts.HORIZONTAL_CARD_WIDTH else HaConsts.VERTICAL_CARD_WIDTH,
        cardHeight = if (horizontal) HaConsts.HORIZONTAL_CARD_HEIGHT else HaConsts.VERTICAL_CARD_HEIGHT,
    )
}

private fun parseRowHorizontalItems(
    wrapper: Element,
    otherCssQuery: String = ""
): List<MediaCard> {
    val cards = mutableListOf<MediaCard>()
    val elements = wrapper.select(".multiple-link-wrapper$otherCssQuery")
    val mediaIdSet = mutableSetOf<String>()
    for (el in elements) {
        val id = el.selectFirst("a[href^=\"https://hanime1.me/watch?v=\"]")
            ?.attr("href")?.toHttpUrlOrNull()?.queryParameter("v")
        val imgUrl = el.select("img").last()?.attr("src")
        val title = el.selectFirst(".card-mobile-title")?.text()?.trim()
        val author = el.selectFirst(".card-mobile-user")?.text()?.trim()
//        val desc = el.select(".card-mobile-duration").joinToString(" ") {
//            it.text()
//        }
        if (id != null && imgUrl != null && title != null && !mediaIdSet.contains(id)) {
            cards.add(
                MediaCard(
                    id = id,
                    title = title,
                    subTitle = author,
                    detailUrl = id,
                    coverImageUrl = imgUrl,
                )
            )
            mediaIdSet.add(id)
        }
    }
    return cards
}

private fun parseRowVerticalItems(wrapper: Element): List<MediaCard> {
    val cards = mutableListOf<MediaCard>()
    val elements = wrapper.select(".home-rows-videos-div")
    val mediaIdSet = mutableSetOf<String>()
    for (el in elements) {
        if (el.parent() != null && el.parent()!!.`is`("a[href^=\"${HaConsts.WATCH_URL}?v=\"]")) {
            val id = el.parent()?.attr("href")?.toHttpUrlOrNull()?.queryParameter("v")
            val imgUrl = el.selectFirst("img")?.attr("src")
            val title = el.selectFirst(".home-rows-videos-title")?.text()?.trim()
            if (id != null && imgUrl != null && title != null && !mediaIdSet.contains(id)) {
                cards.add(
                    MediaCard(
                        id = id,
                        title = title,
                        detailUrl = id,
                        coverImageUrl = imgUrl,
                    )
                )
                mediaIdSet.add(id)
            }
        }
    }
    return cards
}

fun parseWatchPageBody(body: Element): MediaDetail {
    val id = body.selectFirst(Evaluator.Id("video-id"))?.`val`()!!
    val videoEl = body.selectFirst(Evaluator.Id("player"))!!
    val posterImageUrl = videoEl.attr("poster")
    val videoSourceELs = videoEl.select("source[src]")
    val playUrl = if (videoSourceELs.isNotEmpty()) {
        val videoSourceEl = videoEl.select("source[src]").maxByOrNull {
            it.attr("size").toIntOrNull() ?: 0
        }
        videoSourceEl?.attr("src")!!
    } else if (videoEl.attr("src").isNotBlank()) {
        videoEl.attr("src")
    } else {
        VideoSourceUrlPattern.find(videoEl.nextElementSiblings().select("script").html())!!
            .groups[1]!!
            .value
    }
    val author = body.selectFirst(Evaluator.Id("video-artist-name"))?.text()?.trim() ?: ""
    val detailEl = body.selectFirst(".video-details-wrapper .video-description-panel")!!
    val title = detailEl.child(1).text().trim()
    val desc = detailEl.child(2).text().trim()
    val playlistEl = body.selectFirst(Evaluator.Id("playlist-scroll"))
    val tagsWrapperEl = body.selectFirst(".video-details-wrapper.video-tags-wrapper")
    val tags = tagsWrapperEl?.let {
        it.select(".single-video-tag:not([data-toggle])")
            .map { el ->
                el.text().trim()
            }.filter { text ->
                text.isNotEmpty()
            }
    } ?: emptyList()
    val rows = if (playlistEl != null)
        listOf(
            MediaCardRow(
                title = title,
                list = parseRowHorizontalItems(playlistEl, ".related-watch-wrap"),
                cardWidth = HaConsts.HORIZONTAL_CARD_WIDTH,
                cardHeight = HaConsts.HORIZONTAL_CARD_HEIGHT,
            )
        )
    else emptyList()
    return MediaDetail(
        id = id,
        title = title,
        subTitle = author,
        description = buildString {
            if (tags.isNotEmpty()) {
                append(tags.joinToString(" | "))
                append("\n")
            }
            append(desc)
        },
        detailUrl = id,
        backgroundImageUrl = posterImageUrl,
        playSourceList = listOf(
            MediaPlaySource(
                id = "HA",
                name = "HA",
                episodeList = listOf(
                    MediaEpisode(
                        id = id,
                        name = "播放",
                        flag5 = playUrl
                    )
                )
            )
        ),
        favoritedMediaCard = SavedMediaCard(
            id = id,
            title = title,
            detailUrl = id,
            coverImageUrl = posterImageUrl,
            cardWidth = HaConsts.HORIZONTAL_CARD_WIDTH,
            cardHeight = HaConsts.HORIZONTAL_CARD_HEIGHT,
        ),
        rows = rows,
    )
}

fun parseSearchOptionsFromSearchPage(body: Element): List<MediaCatalogOption> {
    val genreOption = parseGenresFromSearchPage(body)
    val tagOptions = parseTagsFromSearchPage(body)
    return buildList {
        add(genreOption)
        addAll(tagOptions)
    }
}

fun parseGenresFromSearchPage(body: Element): MediaCatalogOption {
    val genresEl = body.selectFirst("#genre-modal .modal-body")!!
    return MediaCatalogOption(
        name = "类型",
        value = "genre",
        items = genresEl.select(".hentai-sort-options").mapIndexed { index, item ->
            val name = item.text().trim()
            MediaCatalogOptionItem(
                name = name,
                value = name,
                defaultChecked = index == 0,
            )
        },
        required = true,
    )
}

fun parseTagsFromSearchPage(body: Element): List<MediaCatalogOption> {
    val options = mutableListOf<MediaCatalogOption>()
    val tagsEl = body.selectFirst("#tags .modal-body")!!
    tagsEl.children().toList().filter {
        it.`is`("h5") || it.`is`("label")
    }.forEach {
        if (it.`is`("h5")) {
            val name = it.text().trim()
            options.add(
                MediaCatalogOption(
                    name = name,
                    value = "$TagOptionPrefix$name",
                    items = mutableListOf(),
                    multiple = true,
                )
            )
        } else {
            val tag = it.selectFirst("input[name=\"tags[]\"]")!!.`val`().trim()
            (options.last().items as MutableList<MediaCatalogOptionItem>)
                .add(
                    MediaCatalogOptionItem(
                        name = tag,
                        value = tag,
                    )
                )
        }
    }
    return options
}

fun parsePagedVideosFromSearchPage(body: Element): Pair<PagingResult<MediaCard>, Boolean> {
    val wrapper = body.selectFirst(Evaluator.Id("home-rows-wrapper"))!!
    val horizontal = wrapper.selectFirst(".home-rows-videos-div") == null
    val cards = if (horizontal)
        parseRowHorizontalItems(wrapper, ".search-doujin-videos")
    else
        parseRowVerticalItems(wrapper)
    val paginationEl = body.selectFirst("ul.pagination[role=\"navigation\"]")
    var page = 1
    var maxPage = 1
    paginationEl?.let {
        it.selectFirst("li.page-item.active")?.let { activeEl ->
            val text = activeEl.text().trim()
            if (DigitsPattern.matches(text)) {
                page = text.toInt()
                maxPage = page
            }
        }
        it.select("li.page-item").toList()
            .map { it.text().trim() }
            .filter { DigitsPattern.matches(it) }
            .map { it.toInt() }
            .let { pageNoList ->
                if (pageNoList.isNotEmpty()) {
                    maxPage = pageNoList.max()
                }
            }
    }
    return Pair(
        PagingResult<MediaCard>(
            list = cards,
            prevKey = if (page > 1) "${page - 1}" else null,
            nextKey = if (page < maxPage) "${page + 1}" else null
        ),
        horizontal
    )
}