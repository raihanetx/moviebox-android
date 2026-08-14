package com.moviebox.downloader.network

import com.google.gson.annotations.SerializedName

/**
 * Response from POST /api/extract — extracted video info from yt-dlp.
 */
data class ExtractResponse(
    @SerializedName("title") val title: String,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("webpageUrl") val webpageUrl: String,
    @SerializedName("uploader") val uploader: String? = null,
    @SerializedName("extractor") val extractor: String? = null,
    @SerializedName("isSeries") val isSeries: Boolean = false,
    @SerializedName("seriesSubjectId") val seriesSubjectId: String? = null,
    @SerializedName("currentSeason") val currentSeason: Int? = null,
    @SerializedName("currentEpisode") val currentEpisode: Int? = null,
    @SerializedName("seasons") val seasons: List<SeasonInfo>? = null,
    @SerializedName("formats") val formats: List<VideoFormat>,
    @SerializedName("dubs") val dubs: List<DubInfo>? = null,
)

data class SeasonInfo(
    @SerializedName("se") val se: Int,
    @SerializedName("episodeCount") val episodeCount: Int,
    @SerializedName("resolutions") val resolutions: List<Int>? = null,
)

data class VideoFormat(
    @SerializedName("formatId") val formatId: String,
    @SerializedName("ext") val ext: String,
    @SerializedName("resolution") val resolution: String,
    @SerializedName("fps") val fps: Int? = null,
    @SerializedName("vcodec") val vcodec: String,
    @SerializedName("acodec") val acodec: String,
    @SerializedName("filesize") val filesize: Long? = null,
    @SerializedName("filesizeApprox") val filesizeApprox: Long? = null,
    @SerializedName("label") val label: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("_origUrl") val origUrl: String? = null,
) {
    val directUrl: String get() = origUrl ?: url ?: ""
    val totalBytes: Long get() = filesize ?: filesizeApprox ?: 0L
}

data class DubInfo(
    @SerializedName("lanCode") val lanCode: String,
    @SerializedName("lanName") val lanName: String,
    @SerializedName("subjectId") val subjectId: String? = null,
    @SerializedName("type") val type: Int = 0,
    @SerializedName("original") val original: Boolean = false,
)

/**
 * Response from POST /api/preload — start background download.
 */
data class PreloadStartResponse(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Long,
    @SerializedName("ext") val ext: String,
)

/**
 * Response from GET /api/preload?id=...&status=1 — poll progress.
 */
data class PreloadStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("phase") val phase: String? = null,
    @SerializedName("progress") val progress: Int = 0,
    @SerializedName("downloaded") val downloaded: Long = 0L,
    @SerializedName("total") val total: Long = 0L,
    @SerializedName("speedBps") val speedBps: Long = 0L,
    @SerializedName("error") val error: String? = null,
)
