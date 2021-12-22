package io.datax.shared

import io.datax.shared.repo.DatabaseDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WorkflowManager<D>(
    preferences: Preferences? = null,
    databaseDriverFactory: DatabaseDriverFactory,
    formDataUploadDelegate: FormDataUploadDelegate<D>,
    spotifyClientId: String? = null,
    parcelAppId: String? = null,
    parcelClientId: String? = null,
    pygridHost: String? = null,
    pygridAuthToken: String? = null,
) {

    companion object {

        const val callbackSchema = "spotifyrecommendationlab"
    }

    val spotifyHelper = SpotifyHelper(
        preferences = preferences,
        clientId = spotifyClientId ?: preferences?.getSpotifyClient()!!,
        token = preferences?.getSpotifyToken(),
    )

    val parcelHelper = ParcelHelper(
        preferences = preferences,
        formDataUploadDelegate = formDataUploadDelegate,
        appId = parcelAppId ?: preferences?.getParcelAppId()!!,
        clientId = parcelClientId ?: preferences?.getParcelClientId()!!,
        token = preferences?.getParcelToken(),
    )

    val pygridHelper = PygridHelper(
        preferences = preferences,
        host = pygridHost ?: preferences?.getPygridHost()!!,
        authToken = pygridAuthToken ?: preferences?.getPygridToken()!!,
    )

    val spotifyHistoryFetcher: SpotifyHistoryFetcher = SpotifyHistoryFetcher(databaseDriverFactory)

    val trainingReadinessFlow: Flow<Boolean>
        get() = combine(
            parcelHelper.userFlow.map { it?.id != null },
            spotifyHistoryFetcher.statusFlow.map { it != null && it.trackCount > 0 },
        ) { readiness -> readiness.all { it } }

    var callback: WorkflowManagerCallback? = null
        set(value) {
            field = value
            CoroutineScope(Dispatchers.Main).launch {
                combine(
                    spotifyHelper.userFlow,
                    spotifyHistoryFetcher.statusFlow,
                    parcelHelper.userFlow,
                    trainingReadinessFlow,
                ) { parts -> parts }.collect { parts ->
                    value?.onSpotifyUserChanged(parts[0] as SpotifyUser?)
                    value?.onSpotifyHistoryStatusChanged(parts[1] as SpotifyHistoryStatus?)
                    value?.onParcelUserChanged(parts[2] as ParcelUser?)
                    value?.onTrainingReadinessChanged(parts[3] as Boolean)
                }
            }
        }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            spotifyHelper.refreshCurrentUser()
            loadCachedTrackFeatures()
            parcelHelper.refreshCurrentUser()
        }
    }

    /**
     * Spotify history
     */

    private fun loadCachedTrackFeatures() = spotifyHistoryFetcher.loadCachedTrackFeatures()

    suspend fun fetchHistory(): SpotifyHistoryStatus? = spotifyHelper.token
        ?.let { kotlin.runCatching { spotifyHistoryFetcher.fetchHistory(it) } }
        ?.onFailure { it.printStackTrace() }
        ?.getOrNull()

}
