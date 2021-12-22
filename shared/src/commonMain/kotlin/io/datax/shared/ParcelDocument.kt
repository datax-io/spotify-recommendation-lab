package io.datax.shared

import kotlinx.serialization.Serializable

@Serializable
data class ParcelDocument(
    val id: String,
    val creator: String,
    val owner: String,
    val size: Long,
    val details: ParcelDocumentDetails,
)

@Serializable
data class ParcelDocumentDetails(
    val title: String? = null,
    val tags: List<String> = listOf(),
)
