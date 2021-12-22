package io.datax.shared

data class TrackTrainingData(
    val score: Float,
    val user: Int,
    val acousticness: Float,
    val danceability: Float,
    val durationMs: Float,
    val energy: Float,
    val instrumentalness: Float,
    val liveness: Float,
    val loudness: Float,
    val speechiness: Float,
    val tempo: Float,
    val valence: Float,
)
