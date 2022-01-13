package io.datax.shared

import io.datax.shared.repo.DatabaseDriverFactory
import io.datax.shared.repo.TrackRepo
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import kotlin.math.pow
import kotlin.math.sqrt

suspend fun <T> withRetry(block: suspend () -> T): T = flow { emit(block()) }
    .retry {
        when ((it as? ClientRequestException)?.response?.status) {
            HttpStatusCode.TooManyRequests -> {
                println("Rate limited. Will retry after delay.")
                kotlinx.coroutines.delay(1000L)
                true
            }
            else -> false
        }
    }
    .first()

suspend fun Flow<Track>.toIdSet(): Set<String> = this.map { it.id }.toSet()
fun Sequence<Track>.toIdSet(): Set<String> = this.map { it.id }.toSet()

class SpotifyHistoryFetcher(
    private val preferences: Preferences? = null,
    externalDataPrefix: String? = null,
    databaseDriverFactory: DatabaseDriverFactory,
    private val remoteDataFetcher: RemoteDataFetcher? = null,
) : Changeable() {

    companion object {

        private suspend fun <T> paginate(pageFn: suspend (offset: Int) -> List<T>): Flow<T> = flow {
            var offset = 0
            do {
                val items: List<T> = withRetry { pageFn(offset) }
                items.forEach { emit(it) }
                offset += items.size
            } while (items.isNotEmpty())
        }
    }

    var externalDataPrefix: String? = externalDataPrefix
        set(value) {
            field = value
            preferences?.saveExternalDataPrefix(value)
            notifyChanged()
        }

    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private lateinit var getHeader: (HeadersBuilder.() -> Unit)

    private val pageLimit = 49
    private val baseUrl = "https://api.spotify.com/v1"

    private val trackRepo: TrackRepo = TrackRepo(databaseDriverFactory)
    private var externalDataCount = mutableMapOf<Int, Int>()

    suspend fun loadExternalData(numOfParticipants: Int): SpotifyHistoryStatus {

        val allExternalDataCachedTracks = mutableSetOf<CachedTrack>()
        val allExternalDataTrackFeatures = mutableSetOf<TrackFeature>()

        externalDataCount.clear()

        for (participantId in 1..numOfParticipants) {
            val url = "$externalDataPrefix/$participantId.csv"
            println("Loading data from $url")
            var trackCastingErrorCount = 0
            trackRepo.clearTracksByUserId(participantId.toString())
            remoteDataFetcher?.fetchCsv(url)
                ?.mapNotNull { trackObj ->
                    runCatching {
                        trackObj.asCacheTrack(participantId) to trackObj.asFeature()
                    }.onFailure {
                        println("Track: $trackObj")
                        println("Error occurred while casting: $it")
                        trackCastingErrorCount += 1
                    }.getOrNull()
                }
                ?.forEach { (track, feature) ->
                    allExternalDataCachedTracks.add(track)
                    allExternalDataTrackFeatures.add(feature)
                }
            trackCastingErrorCount
                .takeIf { it > 0 }
                ?.also { println("Track casting failed for participant $participantId: $it tracks") }
        }

        allExternalDataCachedTracks
            .asSequence()
            .chunked(800)
            .asFlow()
            .collect { trackRepo.cacheTracks(it) }

        allExternalDataTrackFeatures
            .asSequence()
            .chunked(800)
            .asFlow()
            .collect { trackRepo.cacheTrackFeatures(it.toSet()) }

        for (participantId in 1..numOfParticipants) {
            externalDataCount[participantId] =
                trackRepo.getAvailableTrackCountByUserId(userId = participantId.toString())
        }

        val status = getStatus()
        notifyChanged()
        return status
    }

    suspend fun fetchHistory(spotifyToken: String): SpotifyHistoryStatus {
        getHeader = {
            append(HttpHeaders.Authorization, "Bearer $spotifyToken")
        }

        val currentUserId = getCurrentUserId()

        trackRepo.clearTracksByUserId("0")

        val allTracks = mutableSetOf<Track>()

        val topShortTermTrackIds = getTopTracks(timeRange = "short_term")
            .onEach { allTracks.add(it) }
            .toIdSet()

        val topMediumTermTrackIds = getTopTracks(timeRange = "medium_term")
            .onEach { allTracks.add(it) }
            .toIdSet()

        val topLongTermTrackIds = getTopTracks(timeRange = "long_term")
            .onEach { allTracks.add(it) }
            .toIdSet()

        val recentTrackIds = getRecentTracks()
            .onEach { allTracks.add(it) }
            .toIdSet()

        val savedTrackIds = getSavedTracks()
            .onEach { allTracks.add(it) }
            .toIdSet()

        val albums = mutableSetOf<Album>()
        val inAlbumTrackIds = mutableSetOf<String>()

        val playlists = mutableSetOf<Playlist>()

        val inOwnPlaylistsAlbumIds = mutableSetOf<String>()
        val inForeignPlaylistAlbumIds = mutableSetOf<String>()

        val inOwnPlaylistTrackIds = mutableSetOf<String>()
        val inForeignPlaylistTrackIds = mutableSetOf<String>()

        getSavedAlbums().collect { album ->
            albums.add(album)
            album.tracks?.items?.let { tracks ->
                allTracks.addAll(tracks)
                inAlbumTrackIds.addAll(tracks.map { it.id })
            }
        }

        getPlaylists().collect { playlist ->
            playlists.add(playlist)
            val isOwn = playlist.owner.id == currentUserId

            playlist.tracks.let { tracks ->
                allTracks.addAll(tracks)
                tracks.map { it.id }.let { trackIds ->
                    when (isOwn) {
                        true -> inOwnPlaylistTrackIds.addAll(trackIds)
                        else -> inForeignPlaylistTrackIds.addAll(trackIds)
                    }
                }
                tracks.mapNotNull { it.album?.id }.let { albumIds ->
                    when (isOwn) {
                        true -> inOwnPlaylistsAlbumIds.addAll(albumIds)
                        else -> inForeignPlaylistAlbumIds.addAll(albumIds)
                    }
                }
            }
        }

        allTracks.asSequence().chunked(800).asFlow().collect {
            trackRepo.cacheTracks(
                it.map { track ->
                    val album = allTracks.find { it.id == track.id }?.album
                    CachedTrack(
                        id = track.id + "0",
                        trackId = track.id,
                        userId = "0",
                        inRecentTracks = recentTrackIds.contains(track.id),
                        inSavedTracks = savedTrackIds.contains(track.id),
                        inTopTracksShortTerm = topShortTermTrackIds.contains(track.id),
                        inTopTracksMediumTerm = topMediumTermTrackIds.contains(track.id),
                        inTopTracksLongTerm = topLongTermTrackIds.contains(track.id),
                        inAlbum = inAlbumTrackIds.contains(track.id),
                        inOwnPlaylists = inOwnPlaylistTrackIds.contains(track.id),
                        inForeignPlaylists = inForeignPlaylistTrackIds.contains(track.id),
                        albumInOwnPlaylists = album?.id?.let { inOwnPlaylistsAlbumIds.contains(it) }
                            ?: false,
                        albumInForeignPlaylists = album?.id?.let { inForeignPlaylistAlbumIds.contains(it) }
                            ?: false,
                    )
                })
        }

        allTracks
            .asSequence()
            .map { it.id }
            .chunked(1200)
            .asFlow()
            .map { getTrackFeatures(trackIds = it.toSet()) }
            .collect { trackFeatures ->
                trackRepo.cacheTrackFeatures(trackFeatures)
            }

        val status = getStatus()
        notifyChanged()
        return status
    }

    fun getStatus() = SpotifyHistoryStatus(
        trackCount = trackRepo.getAvailableTrackFeaturesCountByUserId("0"),
        externalDataCount = externalDataCount,
    )

    fun getReadinessForParticipant(participantId: Int): Boolean {
        return trackRepo.getAvailableTrackCountByUserId(participantId.toString()) > 0
    }

    private suspend fun getCurrentUserId(): String = client.get<SpotifyUser>("$baseUrl/me") {
        headers { getHeader() }
    }.id

    private suspend fun getTopTracks(timeRange: String): Flow<Track> = paginate {
        println("Getting top tracks (time_range=$timeRange offset=$it)")
        client.get<TrackList>("$baseUrl/me/top/tracks") {
            headers { getHeader() }
            parameter("limit", pageLimit)
            parameter("offset", it)
            parameter("time_range", timeRange)
        }.items
    }

    private suspend fun getSavedTracks(): Flow<Track> = paginate {
        println("Getting saved tracks (offset=$it)")
        client.get<TrackItemList>("$baseUrl/me/tracks") {
            headers { getHeader() }
            parameter("limit", pageLimit)
            parameter("offset", it)
        }.tracks
    }

    private suspend fun getRecentTracks(): Sequence<Track> {
        println("Getting recent tracks")
        return client.get<TrackItemList>("$baseUrl/me/player/recently-played") {
            headers { getHeader() }
            parameter("limit", pageLimit)
        }.tracks.asSequence()
    }

    private suspend fun getSavedAlbums(): Flow<Album> = paginate {
        println("Getting saved albums (offset=$it)")
        client.get<AlbumItemList>("$baseUrl/me/albums") {
            headers { getHeader() }
            parameter("limit", pageLimit)
            parameter("offset", it)
        }.albums
    }

    private suspend fun getPlaylists(): Flow<Playlist> = paginate { playlistOffset ->
        println("Getting create / saved playlists (offset=$playlistOffset)")
        client.get<PlaylistItemList>("$baseUrl/me/playlists") {
            headers { getHeader() }
            parameter("limit", pageLimit)
            parameter("offset", playlistOffset)
        }.items.map { playlist ->
            playlist.copy(
                trackItemList = paginate { trackOffset ->
                    println("Getting tracks for playlist ${playlist.id} (offset=$trackOffset)")
                    client.get<TrackItemList>("$baseUrl/playlists/${playlist.id}/tracks") {
                        headers { getHeader() }
                        parameter("limit", pageLimit)
                        parameter("offset", trackOffset)
                    }.items
                }.toList().let { TrackItemList(items = it) }
            )
        }
    }

    private suspend fun getTrackFeatures(trackIds: Set<String>): Set<TrackFeature> {
        val tempTrackFeatures = mutableSetOf<TrackFeature>()
        val trackIdsFromCache: Set<String> =
            trackRepo.getCachedTrackFeaturesById(trackIds = trackIds.toList()).map { it.id }.toSet()

        val trackIdsToBeFetched: Set<String> = (trackIds - trackIdsFromCache).toSet()
            .takeIf { it.isNotEmpty() }
            ?: return emptySet()

        println("Getting features of ${trackIdsToBeFetched.size} tracks")

        trackIdsToBeFetched
            .asSequence()
            .chunked(100)
            .asFlow()
            .map { ids ->
                kotlin.runCatching {
                    withRetry {
                        client.get<ListAudioFeatureResponse>("$baseUrl/audio-features") {
                            headers { getHeader() }
                            parameter("ids", ids.joinToString(","))
                        }.trackFeatures.filterNotNull().toSet()
                    }
                }
                    .onFailure { it.printStackTrace() }
                    .getOrThrow()
            }
            .collect { tempTrackFeatures.addAll(it) }

        return tempTrackFeatures
    }

    fun loadCachedTrackFeatures() {
        notifyChanged()
    }

    fun trainingData(participantId: Int, normalizeScores: Boolean = true): List<TrackTrainingData> {
        val tracksMap: Map<String, CachedTrack>
        if (normalizeScores) {
            val trackHelper = TrackHelper()
            tracksMap = trackHelper.resample(trackRepo.getAllCachedTracksByUserId(userId = participantId.toString()))
                .associateBy { it.trackId }
            println("participantId = $participantId")
            println("normalizeScores = $normalizeScores")
            println("normalizedTracksMap: ${tracksMap.size}")
        } else {
            tracksMap =
                trackRepo.getAllCachedTracksByUserId(userId = participantId.toString()).associateBy { it.trackId }
            println("participantId = $participantId")
            println("normalizeScores = $normalizeScores")
            println("tracksMap: ${tracksMap.size}")
        }
        val featuresMap = trackRepo.getAllCachedTrackFeatures().associateBy { it.id }
        println("featuresMap: ${featuresMap.size}")
        val trackIds = tracksMap.keys intersect featuresMap.keys

        val features = featuresMap.filterValues { it.id in trackIds }.values

        val acousticnessMean = features.map { it.acousticness }.average().toFloat()
        val danceabilityMean = features.map { it.danceability }.average().toFloat()
        val durationMsMean = features.map { it.durationMs.toFloat() }.average().toFloat()
        val energyMean = features.map { it.energy }.average().toFloat()
        val instrumentalnessMean = features.map { it.instrumentalness }.average().toFloat()
        val livenessMean = features.map { it.liveness }.average().toFloat()
        val loudnessMean = features.map { it.loudness }.average().toFloat()
        val speechinessMean = features.map { it.speechiness }.average().toFloat()
        val tempoMean = features.map { it.tempo }.average().toFloat()
        val valenceMean = features.map { it.valence }.average().toFloat()

        val acousticnessStdDev = features.map { it.acousticness }.sd()
        val danceabilityStdDev = features.map { it.danceability }.sd()
        val durationMsStdDev = features.map { it.durationMs.toFloat() }.sd()
        val energyStdDev = features.map { it.energy }.sd()
        val instrumentalnessStdDev = features.map { it.instrumentalness }.sd()
        val livenessStdDev = features.map { it.liveness }.sd()
        val loudnessStdDev = features.map { it.loudness }.sd()
        val speechinessStdDev = features.map { it.speechiness }.sd()
        val tempoStdDev = features.map { it.tempo }.sd()
        val valenceStdDev = features.map { it.valence }.sd()

        return trackIds
            .map { tracksMap[it]!! to featuresMap[it]!! }
            .map { (track, feature) ->
                TrackTrainingData(
                    score = track.score,
                    normalizedScore = track.normalizedScore,
                    user = participantId,
                    acousticness = (feature.acousticness - acousticnessMean) / acousticnessStdDev,
                    danceability = (feature.danceability - danceabilityMean) / danceabilityStdDev,
                    durationMs = (feature.durationMs.toFloat() - durationMsMean) / durationMsStdDev,
                    energy = (feature.energy - energyMean) / energyStdDev,
                    instrumentalness = (feature.instrumentalness - instrumentalnessMean) / instrumentalnessStdDev,
                    liveness = (feature.liveness - livenessMean) / livenessStdDev,
                    loudness = (feature.loudness - loudnessMean) / loudnessStdDev,
                    speechiness = (feature.speechiness - speechinessMean) / speechinessStdDev,
                    tempo = (feature.tempo - tempoMean) / tempoStdDev,
                    valence = (feature.valence - valenceMean) / valenceStdDev,
                )
            }
    }

}

fun Collection<Float>.sd(): Float {
    val mean = this.average().toFloat()
    return this
        .fold(0.0f) { accumulator, next -> accumulator + (next - mean).pow(2.0f) }
        .let { sqrt(it / this.size) }
}
