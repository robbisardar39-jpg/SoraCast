package com.robbi.soracast.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.PROVIDER_STATUS_DOWN
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnifiedHomeController(
    private val aggregator: UnifiedShelfAggregator = UnifiedShelfAggregator
) {
    private val _allShelves = MutableStateFlow<List<AggregatedShelf>>(emptyList())
    val allShelves: StateFlow<List<AggregatedShelf>> = _allShelves.asStateFlow()

    private val _visibleShelves = MutableStateFlow<List<AggregatedShelf>>(emptyList())
    val visibleShelves: StateFlow<List<AggregatedShelf>> = _visibleShelves.asStateFlow()

    private val _loadState = MutableStateFlow<Resource<List<AggregatedShelf>>>(Resource.Loading())
    val loadState: StateFlow<Resource<List<AggregatedShelf>>> = _loadState.asStateFlow()

    private val _providerCounts = MutableStateFlow(0 to 0)
    val providerCounts: StateFlow<Pair<Int, Int>> = _providerCounts.asStateFlow()

    private val _statusMessage = MutableStateFlow("Loading plugins...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SoraCastConfig.categoryFilter)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val isBootstrapping = AtomicBoolean(false)

    private val onPluginsLoaded: (Boolean) -> Unit = { _ ->
        refreshShelves(reloadPlugins = false)
    }

    init {
        MainActivity.afterPluginsLoadedEvent += onPluginsLoaded
        refreshShelves(reloadPlugins = false)
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun bootstrapAndLoad(context: Context, forceReinstall: Boolean = false) {
        if (!SoraCastConfig.unifiedShelvesEnabled) return
        if (!isBootstrapping.compareAndSet(false, true)) return

        _loadState.value = Resource.Loading()
        ioSafe {
            try {
                val activity = context.findActivity()
                val primaryRepo = SoraCastConfig.defaultRepositories.first()
                val existingUrls = RepositoryManager.getRepositories().map { it.url }.toSet()
                if (primaryRepo.url !in existingUrls) {
                    RepositoryManager.addRepository(primaryRepo)
                }

                if (activity != null) {
                    _statusMessage.value = "Syncing plugins..."
                    val repoPlugins = runCatching {
                        RepositoryManager.getRepoPlugins(primaryRepo).orEmpty()
                    }.getOrDefault(emptyList())

                    val extensionsToAdd = repoPlugins.filter { (_, _, metadata) ->
                        SoraCastConfig.isRecordedExtension(metadata.internalName) ||
                            SoraCastConfig.isRecordedExtension(metadata.name) ||
                            SoraCastConfig.normalizeToken(metadata.internalName).contains("livesport") ||
                            SoraCastConfig.normalizeToken(metadata.name).contains("livesport")
                    }

                    extensionsToAdd.amap { (_, repoData, metadata) ->
                        val targetFile = PluginManager.getPluginPath(
                            activity,
                            metadata.internalName,
                            repoData.url
                        )
                        val isLiveSports = SoraCastConfig.normalizeToken(metadata.internalName).contains("livesport") ||
                            SoraCastConfig.normalizeToken(metadata.name).contains("livesport")
                        val alreadyDownloaded = !forceReinstall && !isLiveSports && targetFile.exists()
                        if (!alreadyDownloaded && metadata.status != PROVIDER_STATUS_DOWN) {
                            runCatching {
                                PluginManager.downloadPlugin(
                                    activity = activity,
                                    pluginUrl = metadata.url,
                                    pluginHash = metadata.fileHash,
                                    internalName = metadata.internalName,
                                    repositoryUrl = repoData.url,
                                    loadPlugin = true
                                )
                            }
                        }
                    }

                    runCatching {
                        PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllOnlinePlugins(activity)
                    }
                }

                val snapshot = aggregator.loadUnifiedSnapshot(page = 1)
                _providerCounts.value = snapshot.totalLoadedApis to snapshot.mainPageProviders
                _allShelves.value = snapshot.shelves
                applyFilter()
                _statusMessage.value = "${snapshot.mainPageProviders} active"
                _loadState.value = Resource.Success(snapshot.shelves)
            } catch (t: Throwable) {
                _statusMessage.value = "Plugin sync error"
                _loadState.value = Resource.Failure(
                    isNetworkError = t is IOException,
                    errorString = t.localizedMessage ?: "Failed to load plugins"
                )
            } finally {
                isBootstrapping.set(false)
            }
        }
    }

    fun refreshShelves(
        page: Int = 1,
        reloadPlugins: Boolean = false,
        context: Context? = null
    ) {
        if (!SoraCastConfig.unifiedShelvesEnabled) {
            _allShelves.value = emptyList()
            _visibleShelves.value = emptyList()
            _loadState.value = Resource.Success(emptyList())
            return
        }

        if (reloadPlugins && context != null) {
            bootstrapAndLoad(context = context, forceReinstall = false)
            return
        }

        _loadState.value = Resource.Loading()
        ioSafe {
            try {
                val snapshot = aggregator.loadUnifiedSnapshot(page = page)
                _providerCounts.value = snapshot.totalLoadedApis to snapshot.mainPageProviders
                _allShelves.value = snapshot.shelves
                applyFilter()
                _statusMessage.value = "${snapshot.mainPageProviders} active"
                _loadState.value = Resource.Success(snapshot.shelves)
            } catch (t: Throwable) {
                _loadState.value = Resource.Failure(
                    isNetworkError = t is IOException,
                    errorString = t.localizedMessage ?: "Failed to load shelves"
                )
            }
        }
    }

    fun setCategoryFilter(filterName: String) {
        SoraCastConfig.categoryFilter = filterName
        _selectedCategory.value = filterName
        applyFilter()
    }

    fun setUnifiedShelvesEnabled(enabled: Boolean) {
        SoraCastConfig.unifiedShelvesEnabled = enabled
        refreshShelves()
    }

    private fun applyFilter() {
        val current = _allShelves.value
        _visibleShelves.value = when (_selectedCategory.value.uppercase()) {
            "MOVIES" -> current.filter { it.bucket == ShelfBucket.MOVIES }
            "ANIME" -> current.filter { it.bucket == ShelfBucket.ANIME }
            "LIVE" -> current.filter { shelf ->
                shelf.bucket == ShelfBucket.LIVE && shelf.providerNames.none {
                    it.equals("Eclipsia", ignoreCase = true) && !shelf.title.lowercase().contains("live")
                }
            }
            else -> current
        }
    }

    fun release() {
        MainActivity.afterPluginsLoadedEvent -= onPluginsLoaded
    }
}
