package io.datax.shared

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val redirectUri = "https://storage.googleapis.com/datax-research-public/parcel-redirect/index.html"

class ParcelHelper<D>(
    private val preferences: Preferences? = null,
    private val formDataUploadDelegate: FormDataUploadDelegate<D>,
    appId: String,
    clientId: String,
    token: String? = null,
) {

    companion object {

        private const val baseUrl = "https://api.oasislabs.com/parcel/v1"

        private val jsonCodec = Json { ignoreUnknownKeys = true }

    }

    var appId = appId
        set(value) {
            field = value
            this.token = null
        }

    var clientId = clientId
        set(value) {
            field = value
            this.token = null
        }

    private var userId: String? = null

    var token: String? = token
        set(value) {
            field = value
            preferences?.saveParcelToken(value)
        }

    private val userChannel = MutableStateFlow<ParcelUser?>(null)
    val userFlow get() = userChannel.asStateFlow()


    /**
     * See https://docs.oasislabs.com/parcel/latest/selected-topics/login-with-oasis.html#oasis-auth-for-static-single-page-applications
     */
    fun getAuthUrl(): String = OpenIDHelper.getUri(
        clientId = clientId,
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
        ?.also { userId = it.id }
        .also { userChannel.tryEmit(it) }

    /**
     * https://docs.oasislabs.com/parcel/latest/parcel-api.html#operation/uploadDocument
     */
    suspend fun uploadDocument(data: D, fileName: String): ParcelDocument {
        val metadata = jsonObjectOf(
            "details" to mapOf(
                "title" to fileName,
                "tags" to listOf("to-app-$appId"),
            )
        )
        return DocumentUploadData(
            url = "https://storage.oasislabs.com/v1/parcel",
            token = token!!,
        ).let {
            kotlin.runCatching {
                formDataUploadDelegate.uploadFormData(it.url, it.token, data, metadata.toString())
                    .let { jsonCodec.decodeFromString<ParcelDocument>(it) }
            }
        }.onFailure { it.printStackTrace() }.getOrThrow()
    }
}