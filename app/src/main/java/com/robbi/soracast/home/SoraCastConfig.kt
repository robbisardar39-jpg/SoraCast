package com.robbi.soracast.home

import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData

object SoraCastConfig {
    private const val KEY_UNIFIED_SHELVES = "soracast_unified_shelves_enabled"
    private const val KEY_CATEGORY_FILTER = "soracast_home_category_filter"

    const val PRIMARY_REPO_URL = "https://cs.eclipsia.dpdns.org/repo.json"

    val recordedExtensions: Set<String> = setOf(
        "Eclipsia",
        "EclipsiaAnime",
        "LiveSports"
    )

    val animeProviderNames: Set<String> = setOf(
        "Ani.Pm",
        "Anikoto",
        "AniZone",
        "AniKotoAnime",
        "EclipsiaAnime",
        "EclipsiaKitsu",
        "EclipsiaAnilist"
    )

    val moviesProviderNames: Set<String> = setOf(
        "BanglaPlex",
        "Cinejoy",
        "MovieBox",
        "Eclipsia",
        "EclipsiaIMDb",
        "EclipsiaBDIX"
    )

    val liveProviderNames: Set<String> = setOf(
        "LiveSports",
        "Playz",
        "LiveScore",
        "LiveTV",
        "Sports"
    )

    fun normalizeToken(value: String?): String =
        value.orEmpty().lowercase().replace(Regex("[^a-z0-9]"), "")

    fun isRecordedExtension(name: String?): Boolean {
        val clean = normalizeToken(name)
        if (clean.isEmpty()) return false
        return recordedExtensions.any { ext ->
            val norm = normalizeToken(ext)
            clean == norm || clean.contains(norm)
        }
    }

    fun bucketForProviderName(name: String?): ShelfBucket? {
        val clean = normalizeToken(name)
        if (clean.isEmpty()) return null
        return when {
            liveProviderNames.any {
                val norm = normalizeToken(it)
                clean == norm || clean.contains(norm)
            } -> ShelfBucket.LIVE
            animeProviderNames.any {
                val norm = normalizeToken(it)
                clean == norm || clean.contains(norm)
            } -> ShelfBucket.ANIME
            moviesProviderNames.any {
                val norm = normalizeToken(it)
                clean == norm || clean.contains(norm)
            } -> ShelfBucket.MOVIES
            else -> null
        }
    }

    val defaultRepositories: List<RepositoryData> = listOf(
        RepositoryData(
            iconUrl = null,
            name = "Eclipsia",
            url = PRIMARY_REPO_URL,
            description = "Eclipsia, EclipsiaAnime, LiveSports"
        )
    )

    var unifiedShelvesEnabled: Boolean
        get() = getKey<Boolean>(KEY_UNIFIED_SHELVES) ?: true
        set(value) = setKey(KEY_UNIFIED_SHELVES, value)

    var categoryFilter: String
        get() = getKey<String>(KEY_CATEGORY_FILTER) ?: "All"
        set(value) = setKey(KEY_CATEGORY_FILTER, value)
}
