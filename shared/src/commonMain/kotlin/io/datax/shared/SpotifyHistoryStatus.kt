package io.datax.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SpotifyHistoryStatus(
    val trackCount: Int,
)

data class CachedTrack(
    val id: String,
    var album: String?,
    val inRecentTracks: Boolean,
    val inSavedTracks: Boolean,
    val inTopTracksShortTerm: Boolean,
    val inTopTracksMediumTerm: Boolean,
    val inTopTracksLongTerm: Boolean,
    val inAlbum: Boolean,
    val inOwnPlaylists: Boolean,
    val inForeignPlaylists: Boolean,
    var albumInOwnPlaylists: Boolean?,
    var albumInForeignPlaylists: Boolean?,
) {

    private val inTopTracksAnyTerm: Boolean
        get() = inTopTracksShortTerm || inTopTracksMediumTerm || inTopTracksLongTerm

    private val inSavedOrOwnPlaylists: Boolean
        get() = inSavedTracks || inOwnPlaylists

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
            .fold(1.0f) { acc, (_, value) -> acc + value }
}

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
    @SerialName("time_signature")
    val timeSignature: Int,
    val valence: Float,
)
