package io.datax.shared

expect object OpenIDHelper {

    fun getUri(clientId: String, redirectUri: String, scopes: List<String>): String

    fun getTokenRequest(clientId: String, authCode: String): Any

}
