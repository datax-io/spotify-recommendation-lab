package io.datax.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyUser(
    val id: String,

    @SerialName("display_name")
    val displayName: String? = null,
)
