package io.datax.shared

class PygridHelper(
    private val preferences: Preferences? = null,
    host: String?,
    authToken: String?,
    val modelName: String = "spotify_recommendation",
    val modelVersion: String = "1.0",
    participantId: Int,
    val numOfParticipants: Int = 9,
) : Changeable() {

    var host: String? = host
        set(value) {
            field = value
            value?.let { preferences?.savePygridHost(it) }
            notifyChanged()
        }

    var authToken: String? = authToken
        set(value) {
            field = value
            value?.let { preferences?.savePygridToken(it) }
            notifyChanged()
        }

    var participantId = participantId
        set(value) {
            field = value.coerceAtMost(numOfParticipants)
            preferences?.saveParticipantId(value)
            notifyChanged()
        }

    val ready get() = host != null && authToken != null

}
