package io.datax.shared

import cocoapods.AppAuth.OIDAuthorizationRequest
import cocoapods.AppAuth.OIDGrantTypeAuthorizationCode
import cocoapods.AppAuth.OIDServiceConfiguration
import cocoapods.AppAuth.OIDTokenRequest
import platform.Foundation.NSURL

var serviceConfig = OIDServiceConfiguration(
    authorizationEndpoint = NSURL(string = "https://auth.oasislabs.com/oauth/authorize"),
    tokenEndpoint = NSURL(string = "https://auth.oasislabs.com/oauth/token"),
)

private var nonce: String? = null
private var codeVerifier: String? = null

actual object OpenIDHelper {

    actual fun getUri(clientId: String, redirectUri: String, scopes: List<String>): String = OIDAuthorizationRequest(
        configuration = serviceConfig,
        clientId = clientId,
        scopes = scopes,
        redirectURL = NSURL(string = redirectUri),
        responseType = "code",
        additionalParameters = mapOf(
            "audience" to "https://api.oasislabs.com/parcel"
        ),
    ).also {
        nonce = it.nonce
        codeVerifier = it.codeVerifier
    }.authorizationRequestURL().absoluteString!!

    actual fun getTokenRequest(clientId: String, authCode: String): Any = OIDTokenRequest(
        configuration = serviceConfig,
        grantType = OIDGrantTypeAuthorizationCode!!,
        authorizationCode = authCode,
        redirectURL = NSURL(string = "https://storage.googleapis.com/datax-research-public/parcel-redirect/index.html"),
        clientID = clientId,
        clientSecret = null,
        scope = null,
        refreshToken = null,
        codeVerifier = codeVerifier,
        additionalParameters = mapOf(
            "audience" to "https://api.oasislabs.com/parcel"
        ),
    )

}
