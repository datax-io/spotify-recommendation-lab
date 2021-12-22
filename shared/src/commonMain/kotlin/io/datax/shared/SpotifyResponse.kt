package io.datax.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val name: String? = null,
    var album: Album? = null,
    val features: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Track

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}

@Serializable
data class Album(
    val id: String,
    val name: String,
    val tracks: TrackList? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Album

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val owner: Owner,
    @SerialName("tracks")
    val trackItemList: TrackItemList,
) {

    val tracks: List<Track> get() = trackItemList.tracks

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Playlist

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Serializable
data class ListAudioFeatureResponse(

    @SerialName("audio_features")
    val trackFeatures: List<TrackFeature?>,
)

@Serializable
data class Owner(
    val id: String,

    @SerialName("display_name")
    val displayName: String,
)

@Serializable
data class TrackItem(
    val track: Track?,
)

@Serializable
data class AlbumItem(
    val album: Album,
)

@Serializable
data class TrackItemList(
    val items: List<TrackItem> = listOf(),
) {

    val tracks: List<Track> get() = items.mapNotNull { it.track }

}

@Serializable
data class TrackList(
    val items: List<Track> = listOf(),
)

@Serializable
data class AlbumItemList(
    val items: List<AlbumItem> = listOf(),
) {

    val albums: List<Album> get() = items.mapNotNull { it.album }

}

@Serializable
data class PlaylistItemList(
    val items: List<Playlist> = listOf(),
)
