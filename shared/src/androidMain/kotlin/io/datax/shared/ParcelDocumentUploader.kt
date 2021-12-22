package io.datax.shared

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

val byteArrayUploader = object : FormDataUploadDelegate<ByteArray> {

    override suspend fun uploadFormData(
        url: String,
        token: String,
        data: ByteArray,
        metadata: String,
    ): String =
        MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("data", "file", data.toRequestBody("text/plain".toMediaType()))
            .addFormDataPart("metadata", null, metadata.toRequestBody("application/json".toMediaType()))
            .build()
            .let {
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("content-type", "multipart/form-data")
                    .post(it)
                    .build()
            }
            .let { OkHttpClient.Builder().build().newCall(it) }
            .execute()
            .body?.string()!!

}
