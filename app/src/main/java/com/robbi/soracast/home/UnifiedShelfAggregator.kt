package com.robbi.soracast.home

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.isMovieType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository
import kotlinx.coroutines.withTimeoutOrNull

enum class ShelfBucket {
    ALL,
    MOVIES,
    ANIME,
    LIVE
}

data class AggregatedShelf(
    val title: String,
    val bucket: ShelfBucket,
    val providerNames: List<String>,
    val items: List<SearchResponse>
)

data class AggregationSnapshot(
    val totalLoadedApis: Int,
    val mainPageProviders: Int,
    val shelves: List<AggregatedShelf>
)

object UnifiedShelfAggregator {
    private val animeTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.OVA
    )

    private val liveTypes = setOf(
        TvType.Live
    )

    private val liveKeywords = listOf(
        "live",
        "sport",
        "cricket",
        "football",
        "soccer",
        "basketball",
        "tennis",
        "motorsport",
        "formula",
        "motogp",
        "nfl",
        "nba",
        "match",
        "vs ",
        "tv"
    )

    private fun isLiveShelfTitle(title: String): Boolean {
        val lower = title.lowercase()
        return liveKeywords.any { lower.contains(it) }
    }

    fun classifyProvider(api: MainAPI, shelfTitle: String = ""): ShelfBucket {
        val combinedName = listOfNotNull(api.name, api.sourcePlugin).joinToString(" ").lowercase()
        val isLivePlugin = combinedName.contains("livesport") ||
            combinedName.contains("playz") ||
            api.supportedTypes.any { it in liveTypes }
        if (isLivePlugin) {
            return ShelfBucket.LIVE
        }
        SoraCastConfig.bucketForProviderName(api.name)?.let { return it }
        api.sourcePlugin?.let { path ->
            SoraCastConfig.bucketForProviderName(path)?.let { return it }
        }
        if (shelfTitle.isNotBlank() && isLiveShelfTitle(shelfTitle)) {
            return ShelfBucket.LIVE
        }

        val types = api.supportedTypes
        val hasAnime = types.any { it in animeTypes }
        val hasMovie = types.any { it.isMovieType() || it == TvType.TvSeries }
        return when {
            hasAnime && !hasMovie -> ShelfBucket.ANIME
            hasMovie -> ShelfBucket.MOVIES
            else -> ShelfBucket.MOVIES
        }
    }

    private fun bucketPriority(bucket: ShelfBucket): Int = when (bucket) {
        ShelfBucket.MOVIES -> 0
        ShelfBucket.ANIME -> 1
        ShelfBucket.LIVE -> 2
        ShelfBucket.ALL -> 3
    }

    suspend fun loadUnifiedSnapshot(
        page: Int = 1,
        perProviderTimeoutMs: Long = 10_000L
    ): AggregationSnapshot {
        val allApis = synchronized(APIHolder.apis) { APIHolder.apis.toList() }
        val providers = allApis.filter { it.hasMainPage }
        if (providers.isEmpty()) {
            return AggregationSnapshot(
                totalLoadedApis = allApis.size,
                mainPageProviders = 0,
                shelves = emptyList()
            )
        }

        val results = providers.amap { api ->
            val resource = withTimeoutOrNull(perProviderTimeoutMs) {
                APIRepository(api).getMainPage(page = page, nameIndex = null)
            }
            api to resource
        }

        val grouped = linkedMapOf<Pair<ShelfBucket, String>, MutableList<Pair<String, HomePageList>>>()
        for ((api, resource) in results) {
            if (resource is Resource.Success) {
                resource.value.filterNotNull().forEach { homePage ->
                    homePage.items.filter { it.list.isNotEmpty() }.forEach { shelf ->
                        val cleanTitle = shelf.name.trim().ifEmpty { api.name }
                        val bucket = classifyProvider(api, cleanTitle)
                        val key = bucket to cleanTitle
                        grouped.getOrPut(key) { mutableListOf() }.add(api.name to shelf)
                    }
                }
            }
        }

        val shelves = grouped.map { (key, entries) ->
            val (bucket, shelfName) = key
            val seenUrls = HashSet<String>()
            val mergedItems = mutableListOf<SearchResponse>()
            for ((_, shelf) in entries) {
                for (item in shelf.list) {
                    if (seenUrls.add(item.url)) {
                        mergedItems.add(item)
                    }
                }
            }
            AggregatedShelf(
                title = shelfName,
                bucket = bucket,
                providerNames = entries.map { it.first }.distinct(),
                items = mergedItems
            )
        }.sortedWith(
            compareBy<AggregatedShelf> { bucketPriority(it.bucket) }
                .thenByDescending { shelf ->
                    val fromLive = shelf.providerNames.any {
                        val n = it.lowercase()
                        n.contains("livesport") || n.contains("playz")
                    }
                    if (shelf.bucket == ShelfBucket.LIVE && fromLive) 1_000_000 else shelf.items.size
                }
        )

        return AggregationSnapshot(
            totalLoadedApis = allApis.size,
            mainPageProviders = providers.size,
            shelves = shelves
        )
    }

    suspend fun loadUnifiedShelves(
        page: Int = 1,
        perProviderTimeoutMs: Long = 10_000L
    ): List<AggregatedShelf> = loadUnifiedSnapshot(page, perProviderTimeoutMs).shelves
}
