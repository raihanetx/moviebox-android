package com.moviebox.downloader.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MovieBoxApi {

    @POST("api/extract")
    suspend fun extract(
        @Body body: ExtractRequest,
    ): ExtractResponse

    @POST("api/preload")
    suspend fun preloadStart(
        @Body body: PreloadRequest,
    ): PreloadStartResponse

    @GET("api/preload")
    suspend fun preloadStatus(
        @Query("id") jobId: String,
        @Query("status") status: Int = 1,
    ): PreloadStatusResponse
}

data class ExtractRequest(
    val url: String,
)

data class PreloadRequest(
    val directUrl: String,
    val title: String,
    val ext: String,
    val webpageUrl: String,
)
