package io.datax.shared

import kotlin.math.min

class TrackHelper : Changeable() {

    fun resample(tracks: List<CachedTrack>): List<CachedTrack> {
        val sortedTracksList = tracks.sortedBy { it.score }
        val median = tracks[tracks.size / 2].score
        val (firstTracksList, secondTracksList) = sortedTracksList.partition { it.score > median }
        val normalizedListSize = min(firstTracksList.size, secondTracksList.size)
        return firstTracksList.onEach { it.normalizedScore = 1.0f }.shuffled().subList(0, normalizedListSize)
            .plus(secondTracksList.onEach { it.normalizedScore = 0.0f }.shuffled().subList(0, normalizedListSize));
    }

}
