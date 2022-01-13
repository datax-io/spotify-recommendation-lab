package io.datax.shared

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

const val redirectUri = "https://storage.googleapis.com/datax-research-public/parcel-redirect/index.html"

class ParcelHelper<R, D>(
    private val preferences: Preferences? = null,
    private val openIDHelperDelegate: OpenIDHelperDelegate<R>,
    private val formDataUploadDelegate: FormDataUploadDelegate<D>,
    appId: String?,
    clientId: String?,
    token: String? = null,
) : Changeable() {

    companion object {

        private const val baseUrl = "https://api.oasislabs.com/parcel/v1"

        private val jsonCodec = Json { ignoreUnknownKeys = true }

    }

    var appId: String? = appId
        set(value) {
            field = value
            this.token = null
            this.user = null
            preferences?.saveParcelAppId(value)
            notifyChanged()
        }

    var clientId: String? = clientId
        set(value) {
            field = value
            this.token = null
            this.user = null
            preferences?.saveParcelClientId(value)
            notifyChanged()
        }

    var token: String? = token
        set(value) {
            field = value
            if (value == null) {
                user = null
            }
            preferences?.saveParcelToken(value)
        }

    var user: ParcelUser? = null
        private set(value) {
            field = value
            notifyChanged()
        }

    val ready get() = user != null && token != null

    /**
     * See https://docs.oasislabs.com/parcel/latest/selected-topics/login-with-oasis.html#oasis-auth-for-static-single-page-applications
     */
    fun getAuthUrl(): String = openIDHelperDelegate.getUri(
        clientId = clientId ?: throw  Exception("No client ID set"),
        redirectUri = redirectUri,
        scopes = listOf("openid", "profile", "email", "parcel.public", "parcel.safe", "parcel.full")
    )

    suspend fun handleAuthResult(fragment: String) = fragment
        .also { token = it }
        .let { refreshCurrentUser() }

    private val getHeader: HeadersBuilder.() -> Unit
        get() = { append(HttpHeaders.Authorization, "Bearer $token") }

    /**
     * See https://docs.oasislabs.com/parcel/latest/parcel-api.html#operation/getAuthenticatedIdentity
     */
    suspend fun refreshCurrentUser(): ParcelUser? = runCatching {
        httpClient.get<ParcelUser>("$baseUrl/identities/me") { headers(getHeader) }
    }.onFailure { it.printStackTrace() }.getOrNull()
        ?.also { user = it }
        .also { notifyChanged() }

    /**
     * https://docs.oasislabs.com/parcel/latest/parcel-api.html#operation/uploadDocument
     */
    suspend fun uploadDocument(data: D, fileName: String): ParcelDocument {
        val metadata = jsonObjectOf(
            "details" to mapOf(
                "title" to fileName,
                "tags" to listOf("to-app-${appId ?: throw Exception("No")}"),
            )
        )

        return DocumentUploadData(
            url = "https://storage.oasislabs.com/v1/parcel",
            token = token!!,
        ).let {
            kotlin.runCatching {
                formDataUploadDelegate.uploadFormData(it.url, it.token, data, metadata.toString())
                    .let { jsonCodec.decodeFromString<ParcelDocument>(it) }
                    .also { grantDocumentToApp(it.id) }
            }
        }.onFailure { it.printStackTrace() }.getOrThrow()
    }

    fun getTokenRequest(clientId: String, authCode: String) =
        this.openIDHelperDelegate.getTokenRequest(clientId, authCode)

    private suspend fun grantDocumentToApp(documentId: String) {
        val grantPayload = jsonObjectOf(
            "grantee" to appId!!,
            "condition" to mapOf(
                "document.id" to mapOf("\$eq" to documentId),
            ),
            "capabilities" to "read",
        )
        httpClient.post<JsonObject>("$baseUrl/grants") {
            headers(getHeader)
            contentType(ContentType.Application.Json)
            body = grantPayload
        }
    }

}
