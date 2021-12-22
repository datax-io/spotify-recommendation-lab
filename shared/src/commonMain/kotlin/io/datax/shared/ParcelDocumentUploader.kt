package io.datax.shared

data class DocumentUploadData(
    val url: String,
    val token: String,
)

interface FormDataUploadDelegate<D> {

    suspend fun uploadFormData(
        url: String,
        token: String,
        data: D,
        metadata:String,
    ): String

}
