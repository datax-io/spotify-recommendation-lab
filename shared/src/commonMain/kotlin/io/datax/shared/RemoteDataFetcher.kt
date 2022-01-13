package io.datax.shared

interface RemoteDataFetcher {

    suspend fun fetchCsv(url: String): List<Map<String, String>>
}
