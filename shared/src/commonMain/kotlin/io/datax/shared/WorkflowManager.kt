package io.datax.shared

import io.datax.shared.repo.DatabaseDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class WorkflowManager<R, D>(
    preferences: Preferences? = null,
    databaseDriverFactory: DatabaseDriverFactory,
    openIDHelperDelegate: OpenIDHelperDelegate<R>,
    formDataUploadDelegate: FormDataUploadDelegate<D>,
    remoteDataFetcher: RemoteDataFetcher? = null,
) {

    companion object {

        const val callbackSchema = "spotifyrecommendationlab"
    }

    val spotifyHelper = SpotifyHelper(
        preferences = preferences,
        clientId = preferences?.getSpotifyClient(),
        token = preferences?.getSpotifyToken(),
    )

    val parcelHelper = ParcelHelper(
        preferences = preferences,
        openIDHelperDelegate = openIDHelperDelegate,
        formDataUploadDelegate = formDataUploadDelegate,
        appId = preferences?.getParcelAppId() ?: ParcelHelper.defaultAppId,
        clientId = preferences?.getParcelClientId() ?: ParcelHelper.defaultClientId,
        token = preferences?.getParcelToken(),
    )

    val pygridHelper = PygridHelper(
        preferences = preferences,
        host = preferences?.getPygridHost() ?: PygridHelper.defaultHost,
        authToken = preferences?.getPygridToken() ?: PygridHelper.defaultAuthToken,
        participantId = preferences?.getParticipantId() ?: 1,
    )

    val spotifyHistoryFetcher: SpotifyHistoryFetcher = SpotifyHistoryFetcher(
        preferences = preferences,
        externalDataPrefix = preferences?.getExternalDataPrefix(),
        databaseDriverFactory = databaseDriverFactory,
        remoteDataFetcher = remoteDataFetcher,
    )

    private var changesJob: Job = Job()

    private val changesChannel = BroadcastChannel<Unit>(Channel.Factory.CONFLATED)
    val changesFlow get() = changesChannel.asFlow()

    var callback: WorkflowManagerCallback? = null
        set(value) {
            field = value
            CoroutineScope(Dispatchers.Main).launch {
                changesFlow.collect { value?.onParamsChanged() }
            }
        }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            spotifyHelper.refreshCurrentUser()
            loadCachedTrackFeatures()
            parcelHelper.token?.also { parcelHelper.refreshCurrentUser() }

            changesJob = launch {
                listOf(
                    spotifyHelper,
                    spotifyHistoryFetcher,
                    parcelHelper,
                    pygridHelper,
                ).map(Changeable::changeFlow).merge()
                    .collect { changesChannel.offer(Unit) }
            }
        }
    }

    val ready get() = parcelHelper.ready && pygridHelper.ready && spotifyHistoryFetcher.getStatus().trackCount > 0

    /**
     * Spotify history
     */

    private fun loadCachedTrackFeatures() = spotifyHistoryFetcher.loadCachedTrackFeatures()

    suspend fun fetchHistory(): SpotifyHistoryStatus? = spotifyHelper.token
        ?.let { kotlin.runCatching { spotifyHistoryFetcher.fetchHistory(it) } }
        ?.onFailure { it.printStackTrace() }
        ?.getOrNull()

    suspend fun loadExternalData(): SpotifyHistoryStatus? = spotifyHistoryFetcher.externalDataPrefix
        ?.let {
            kotlin.runCatching {
                println("Kotlin run catching in WorkflowManager.loadExternalData() with externalDataPrefix = ${spotifyHistoryFetcher.externalDataPrefix} and with ${pygridHelper.numOfParticipants} participants")
                spotifyHistoryFetcher.loadExternalData(pygridHelper.numOfParticipants)
            }
        }
        ?.onFailure { it.printStackTrace() }
        ?.getOrNull()

}
