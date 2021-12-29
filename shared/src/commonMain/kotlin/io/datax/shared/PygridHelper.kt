package io.datax.shared

class PygridHelper(
    private val preferences: Preferences? = null,
    host: String?,
    authToken: String?,
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

    val ready get() = host != null && authToken != null

}
