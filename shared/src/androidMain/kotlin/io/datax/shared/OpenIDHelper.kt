package io.datax.shared

import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest


var serviceConfig = AuthorizationServiceConfiguration(
    Uri.parse("https://auth.oasislabs.com/oauth/authorize"),
    Uri.parse("https://auth.oasislabs.com/oauth/token"),
)

actual object OpenIDHelper {

    private var nonce: String? = null
    private var codeVerifier: String? = null

    actual fun getUri(clientId: String, redirectUri: String, scopes: List<String>): String =
        AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        ).setScopes(scopes)
            .setAdditionalParameters(mapOf(
                "audience" to "https://api.oasislabs.com/parcel"
            ))
            .build()
            .also {
                this.nonce = it.nonce
                this.codeVerifier = it.codeVerifier
            }
            .toUri()
            .toString()

    actual fun getTokenRequest(clientId: String, authCode: String): Any = TokenRequest.Builder(serviceConfig, clientId)
        .setAuthorizationCode(authCode)
        .setAdditionalParameters(mapOf(
            "audience" to "https://api.oasislabs.com/parcel"
        ))
        .setRedirectUri(Uri.parse("https://storage.googleapis.com/datax-research-public/parcel-redirect/index.html"))
        .setNonce(nonce)
        .setCodeVerifier(codeVerifier)
        .build()

}
