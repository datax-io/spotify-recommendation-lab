package io.datax.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SpotifyHistoryStatus(
    val trackCount: Int,
    val externalDataCount: Map<Int, Int>,
)

data class CachedTrack(
    val id: String,
    val trackId: String,
    val userId: String,
    val inRecentTracks: Boolean,
    val inSavedTracks: Boolean,
    val inTopTracksShortTerm: Boolean,
    val inTopTracksMediumTerm: Boolean,
    val inTopTracksLongTerm: Boolean,
    val inAlbum: Boolean, // if true, the album containing this track is saved by the user.
    val inOwnPlaylists: Boolean,
    val inForeignPlaylists: Boolean,
    var albumInOwnPlaylists: Boolean?,
    var albumInForeignPlaylists: Boolean?,
) {

    private val inTopTracksAnyTerm: Boolean
        get() = inTopTracksShortTerm || inTopTracksMediumTerm || inTopTracksLongTerm

    private val inSavedOrOwnPlaylists: Boolean
        get() = inSavedTracks || inOwnPlaylists

    var normalizedScore: Float? = null

    // see https://gitlab.com/dogcoin/decentralized-ml/deml-recommendation-system-model-lab/-/blob/master/src/track.py
    val score: Float
        get() = listOf(
            // manual saved or added :)
            inSavedTracks to 1.0f,
            inOwnPlaylists to 1.0f,
            (inSavedTracks && inOwnPlaylists) to 0.5f,
            // top tracks :D
            (inTopTracksShortTerm) to 2.0f,
            (inTopTracksMediumTerm) to 1.75f,
            (inTopTracksLongTerm) to 1.5f,
            (inTopTracksShortTerm && inTopTracksMediumTerm) to 2.0f,
            (inTopTracksShortTerm && inTopTracksLongTerm) to 1.0f,
            // manual plus top :) :D
            ((inAlbum || inOwnPlaylists) && inTopTracksAnyTerm) to 2.0f,
            ((inAlbum || inOwnPlaylists) && inTopTracksAnyTerm) to 2.0f,
            // saved album but not in saved :(
            (inAlbum && !inSavedOrOwnPlaylists) to -1.0f,
            (albumInOwnPlaylists == true && !inSavedOrOwnPlaylists) to -1.0f,
        ).filter { (condition, _) -> condition }
            .fold(0.0f) { acc, (_, value) -> acc + value }

}

fun Map<String, String>.asCacheTrack(participantId: Int) = CachedTrack(
    id = this["id"]!! + participantId.toString(),
    trackId = this["id"]!!,
    userId = participantId.toString(),
    inRecentTracks = this["in_recent"] == "True",
    inSavedTracks = this["in_saved"] == "True",
    inTopTracksShortTerm = this["in_top_tracks_short_term"] == "True",
    inTopTracksMediumTerm = this["in_top_tracks_medium_term"] == "True",
    inTopTracksLongTerm = this["in_top_tracks_long_term"] == "True",
    inAlbum = this["in_album"] == "True",
    inOwnPlaylists = this["in_own_playlists"] == "True",
    inForeignPlaylists = this["in_foreign_playlists"] == "True",
    albumInOwnPlaylists = this["album_in_own_playlists"] == "True",
    albumInForeignPlaylists = this["album_in_foreign_playlists"] == "True",
)

@Serializable
data class TrackFeature(
    val id: String,
    val acousticness: Float,
    val danceability: Float,
    @SerialName("duration_ms")
    val durationMs: Int,
    val energy: Float,
    val instrumentalness: Float,
    val key: Int,
    val liveness: Float,
    val loudness: Float,
    val mode: Int,
    val speechiness: Float,
    val tempo: Float,
    val valence: Float,
)

fun Map<String, String>.asFeature() = TrackFeature(
    id = this["id"]!!,
    acousticness = this["acousticness"]!!.toFloat(),
    danceability = this["danceability"]!!.toFloat(),
    durationMs = this["duration_ms"]!!.toInt(),
    energy = this["energy"]!!.toFloat(),
    instrumentalness = this["instrumentalness"]!!.toFloat(),
    key = this["key"]!!.toInt(),
    liveness = this["liveness"]!!.toFloat(),
    loudness = this["loudness"]!!.toFloat(),
    mode = this["mode"]!!.toInt(),
    speechiness = this["speechiness"]!!.toFloat(),
    tempo = this["tempo"]!!.toFloat(),
    valence = this["valence"]!!.toFloat(),
)
