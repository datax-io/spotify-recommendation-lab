package io.datax.shared

interface OpenIDHelperDelegate<R> {

    fun getUri(clientId: String, redirectUri: String, scopes: List<String>): String

    fun getTokenRequest(clientId: String, authCode: String): R

}
