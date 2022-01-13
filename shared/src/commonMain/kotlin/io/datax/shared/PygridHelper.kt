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

    companion object {

        const val defaultHost = "ws://pygrid.datax.io:7001"
        const val defaultAuthToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.e30.Cn_0cSjCw1QKtcYDx_mYN_q9jO2KkpcUoiVbILmKVB4LUCQvZ7YeuyQ51r9h3562KQoSas_ehbjpz2dw1Dk24hQEoN6ObGxfJDOlemF5flvLO_sqAHJDGGE24JRE4lIAXRK6aGyy4f4kmlICL6wG8sGSpSrkZlrFLOVRJckTptgaiOTIm5Udfmi45NljPBQKVpqXFSmmb3dRy_e8g3l5eBVFLgrBhKPQ1VbNfRK712KlQWs7jJ31fGpW2NxMloO1qcd6rux48quivzQBCvyK8PV5Sqrfw_OMOoNLcSvzePDcZXa2nPHSu3qQIikUdZIeCnkJX-w0t8uEFG3DfH1fVA"
    }

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
