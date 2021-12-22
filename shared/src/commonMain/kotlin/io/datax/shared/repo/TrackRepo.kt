package io.datax.shared.repo

import io.datax.shared.CachedTrack
import io.datax.shared.TrackFeature

class TrackRepo(databaseDriverFactory: DatabaseDriverFactory) {

    private val database = MainDatabase(databaseDriverFactory.createDriver())
    private val databaseQueries = database.mainDatabaseQueries

    fun cacheTracks(tracksToCache: List<CachedTrack>) {
        tracksToCache.forEach { track ->
            databaseQueries.insertTrack(
                track.id,
                track.album,
                track.inRecentTracks,
                track.inSavedTracks,
                track.inTopTracksShortTerm,
                track.inTopTracksMediumTerm,
                track.inTopTracksLongTerm,
                track.inAlbum,
                track.inOwnPlaylists,
                track.inForeignPlaylists,
                track.albumInOwnPlaylists,
                track.albumInForeignPlaylists,
            )
        }
    }

    fun cacheTrackFeatures(features: Set<TrackFeature>) = features.forEach {
        databaseQueries.insertTrackFeature(
            id = it.id,
            acousticness = it.acousticness,
            danceability = it.danceability,
            duration_ms = it.durationMs,
            energy = it.energy,
            instrumentalness = it.instrumentalness,
            key = it.key,
            liveness = it.liveness,
            loudness = it.loudness,
            mode = it.mode,
            speechiness = it.speechiness,
            tempo = it.tempo,
            time_signature = it.timeSignature,
            valence = it.valence,
        )
    }

    fun getAllCachedTracks(): List<CachedTrack> {
        return databaseQueries.selectAllTracks().executeAsList().asCachedTracks()
    }

    fun getAllCachedTrackFeatures(): List<TrackFeature> {
        return databaseQueries.selectAllTrackFeatures().executeAsList().asFeatures()
    }

    fun getCachedTrackFeaturesById(trackIds: List<String>): List<TrackFeature> {
        return databaseQueries.selectAllTrackFeaturesByIds(trackIds).executeAsList().asFeatures()
    }

    fun getAvailableTrackCount(): Int {
        return databaseQueries.countTrackFeatures().executeAsOne().toInt()
    }

    private fun List<Tracks>.asCachedTracks() = this.map { trackFeature ->
        CachedTrack(
            id = trackFeature.id,
            album = trackFeature.album,
            inRecentTracks = trackFeature.in_recent_tracks,
            inSavedTracks = trackFeature.in_saved_tracks,
            inTopTracksShortTerm = trackFeature.in_top_tracks_short_term,
            inTopTracksMediumTerm = trackFeature.in_top_tracks_medium_term,
            inTopTracksLongTerm = trackFeature.in_top_tracks_long_term,
            inAlbum = trackFeature.in_album,
            inOwnPlaylists = trackFeature.in_own_playlists,
            inForeignPlaylists = trackFeature.in_foreign_playlists,
            albumInOwnPlaylists = trackFeature.album_in_own_playlists,
            albumInForeignPlaylists = trackFeature.album_in_foreign_playlists,
        )
    }

    private fun List<TrackFeatures>.asFeatures() = this.map { trackFeature ->
        TrackFeature(
            id = trackFeature.id,
            acousticness = trackFeature.acousticness,
            danceability = trackFeature.danceability,
            durationMs = trackFeature.duration_ms,
            energy = trackFeature.energy,
            instrumentalness = trackFeature.instrumentalness,
            key = trackFeature.key,
            liveness = trackFeature.liveness,
            loudness = trackFeature.loudness,
            mode = trackFeature.mode,
            speechiness = trackFeature.speechiness,
            tempo = trackFeature.tempo,
            timeSignature = trackFeature.time_signature,
            valence = trackFeature.valence
        )
    }

    fun clearTracks() {
        databaseQueries.deleteAllTracks()
    }

    fun clearCache() {
        databaseQueries.deleteAllTracks()
        databaseQueries.deleteAllTrackFeatures()
    }
}
