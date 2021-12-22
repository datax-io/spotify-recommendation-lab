package io.datax.shared

class PygridHelper(
    private val preferences: Preferences? = null,
    host: String,
    authToken: String,
) {

    var host: String = host
        set(value) {
            field = value
            preferences?.savePygridHost(value)
        }

    var authToken: String = authToken
        set(value) {
            field = value
            preferences?.savePygridToken(value)
        }

}
