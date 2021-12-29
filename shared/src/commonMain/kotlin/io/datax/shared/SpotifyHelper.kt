package io.datax.shared

import io.ktor.client.request.*
import io.ktor.http.*

class SpotifyHelper(
    private val preferences: Preferences? = null,
    private val redirectUrl: String = "${WorkflowManager.callbackSchema}://spotifyauth",
    clientId: String?,
    token: String? = null,
) : Changeable() {

    var clientId: String? = clientId
        set(value) {
            field = value
            token = null
            preferences?.saveSpotifyClient(value)
            notifyChanged()
        }

    internal var token: String? = token
        set(value) {
            field = value
            if (value == null) {
                user = null
            }
            preferences?.saveSpotifyToken(value)
        }

    var user: SpotifyUser? = null
        private set(value) {
            field = value
            notifyChanged()
        }

    fun getAuthUrl(): String {
        val clientId = clientId
            ?: throw Exception("Client ID is not set")
        return URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = "accounts.spotify.com",
            encodedPath = "authorize",
            parameters = ParametersBuilder().also {
                it.append("client_id", clientId)
                it.append("response_type", "token")
                it.append("redirect_uri", redirectUrl)
                it.append("show_dialog", "true")
                it.append("scope", listOf(
                    "user-read-recently-played",
                    "user-library-read",
                    "playlist-read-private",
                    "user-top-read",
                ).joinToString(","))
            }
        ).build().toString()
    }

    suspend fun handleAuthResult(fragment: String) = fragment
        .split("&")
        .associateBy({ it.substringBefore("=") }, { it.substringAfter("=") })["access_token"]
        ?.also { token = it }
        ?.let { refreshCurrentUser() }

    private val getHeader: HeadersBuilder.() -> Unit
        get() = { append(HttpHeaders.Authorization, "Bearer $token") }

    suspend fun refreshCurrentUser(): SpotifyUser? = runCatching {
        httpClient.get<SpotifyUser>("https://api.spotify.com/v1/me") { headers(getHeader) }
    }.getOrNull().also {
        this.user = it
    }

}
