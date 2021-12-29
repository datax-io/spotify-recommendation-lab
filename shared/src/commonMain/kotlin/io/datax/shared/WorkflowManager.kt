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
        appId = preferences?.getParcelAppId(),
        clientId = preferences?.getParcelClientId(),
        token = preferences?.getParcelToken(),
    )

    val pygridHelper = PygridHelper(
        preferences = preferences,
        host = preferences?.getPygridHost(),
        authToken = preferences?.getPygridToken(),
    )

    val spotifyHistoryFetcher: SpotifyHistoryFetcher = SpotifyHistoryFetcher(
        databaseDriverFactory = databaseDriverFactory,
        preferences = preferences,
        participantId = preferences?.getParticipantId() ?: 1,
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
            parcelHelper.refreshCurrentUser()

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

}
