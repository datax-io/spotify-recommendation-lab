package io.datax.shared

object PreferenceKey {

    const val spotifyToken = "spotify_token"
    const val spotifyClient = "spotify_client"

    const val parcelToken = "parcel_token"
    const val parcelAppId = "parcel_app_id"
    const val parcelClientId = "parcel_client_id"

    const val participantId = "participant_id"
    const val pygridHost = "pygrid_host"
    const val pygridToken = "pygrid_token"
    const val externalDataPrefix = "external_data_prefix"

}

fun Preferences.getSpotifyClient(): String? = getString(PreferenceKey.spotifyClient)
fun Preferences.saveSpotifyClient(client: String?) = when (client) {
    null -> removeString(PreferenceKey.spotifyClient)
    else -> setString(PreferenceKey.spotifyClient, client)
}

fun Preferences.getSpotifyToken(): String? = getString(PreferenceKey.spotifyToken)
fun Preferences.saveSpotifyToken(token: String?) = when (token) {
    null -> removeString(PreferenceKey.spotifyToken)
    else -> setString(PreferenceKey.spotifyToken, token)
}

fun Preferences.getParcelToken(): String? = getString(PreferenceKey.parcelToken)
fun Preferences.saveParcelToken(token: String?) = when (token) {
    null -> removeString(PreferenceKey.parcelToken)
    else -> setString(PreferenceKey.parcelToken, token)
}

fun Preferences.getParcelAppId(): String? = getString(PreferenceKey.parcelAppId)
fun Preferences.saveParcelAppId(appId: String?) = when (appId) {
    null -> removeString(PreferenceKey.parcelAppId)
    else -> setString(PreferenceKey.parcelAppId, appId)
}

fun Preferences.getParcelClientId(): String? = getString(PreferenceKey.parcelClientId)
fun Preferences.saveParcelClientId(clientId: String?) = when (clientId) {
    null -> removeString(PreferenceKey.parcelClientId)
    else -> setString(PreferenceKey.parcelClientId, clientId)
}

fun Preferences.getParticipantId(): Int = getString(PreferenceKey.participantId)?.toInt() ?: 1
fun Preferences.saveParticipantId(participant: Int) = when (participant) {
    0 -> removeString(PreferenceKey.participantId)
    else -> setString(PreferenceKey.participantId, participant.toString())
}

fun Preferences.getPygridHost(): String? = getString(PreferenceKey.pygridHost)
fun Preferences.savePygridHost(host: String) = setString(PreferenceKey.pygridHost, host)

fun Preferences.getPygridToken(): String? = getString(PreferenceKey.pygridToken)
fun Preferences.savePygridToken(token: String) = setString(PreferenceKey.pygridToken, token)

fun Preferences.getExternalDataPrefix(): String? = getString(PreferenceKey.externalDataPrefix)
fun Preferences.saveExternalDataPrefix(prefix: String?) = when (prefix) {
    null -> removeString(PreferenceKey.externalDataPrefix)
    else -> setString(PreferenceKey.externalDataPrefix, prefix)
}
