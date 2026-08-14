package com.moviebox.downloader.download

import com.moviebox.downloader.network.ApiClient
import com.moviebox.downloader.network.PreloadRequest
import com.moviebox.downloader.network.PreloadStatusResponse
import com.moviebox.downloader.network.VideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * One download job (single file).
 *
 * Phase 1 — server downloads from CDN: progress 0 → 95%, polled via /api/preload?status=1
 * Phase 2 — merge on server: progress 95 → 99%
 * Phase 3 — transfer server → phone: progress 0 → 100% (separate counter)
 */
data class DownloadJob(
    val id: String,
    val title: String,
    val ext: String,
    val totalBytes: Long,
    val webpageUrl: String,
    val directUrl: String,
) {
    var jobId: String? = null
    var phase: Phase = Phase.QUEUED
    var progress: Int = 0
    var downloadedBytes: Long = 0L
    var speedBps: Long = 0L
    var error: String? = null
    var savedFilePath: String? = null
}

enum class Phase {
    QUEUED,           // Just created, not yet started
    FETCHING,         // Server is downloading from CDN
    MERGING,           // Server is merging chunks
    TRANSFERRING,     // Server → phone transfer
    COMPLETED,
    FAILED,
}

/**
 * Manages a single download from start to finish.
 *
 * Flow:
 * 1. POST /api/preload → start server-side download, get jobId
 * 2. Poll /api/preload?status=1 every 2s until status = "ready"
 * 3. GET /api/preload?id=<jobId> in 30MB chunks (Range requests) → save to Downloads/
 *
 * Why chunked Range requests for phase 3?
 * The preview gateway kills any single request at 30s. 30MB at 14 MB/s = 2s, well under 30s.
 */
class DownloadManager(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {

    /**
     * Start a download. Returns a Flow of DownloadJob updates (progress, phase, etc.)
     * The flow completes when the download is done (or fails).
     */
    fun start(
        title: String,
        ext: String,
        webpageUrl: String,
        format: VideoFormat,
        outputDir: File,
    ): Flow<DownloadJob> = flow {
        val job = DownloadJob(
            id = "job-${System.currentTimeMillis()}-${(1000..9999).random()}",
            title = title,
            ext = ext,
            totalBytes = format.totalBytes,
            webpageUrl = webpageUrl,
            directUrl = format.directUrl,
        )
        emit(job)

        try {
            // PHASE 1: Start server-side download
            job.phase = Phase.FETCHING
            emit(job)

            val startRes = ApiClient.api.preloadStart(PreloadRequest(
                directUrl = format.directUrl,
                title = title,
                ext = ext,
                webpageUrl = webpageUrl,
            ))
            job.jobId = startRes.jobId
            emit(job)

            // PHASE 2: Poll status until ready
            while (true) {
                kotlinx.coroutines.delay(2000)
                val status: PreloadStatusResponse = try {
                    ApiClient.api.preloadStatus(startRes.jobId)
                } catch (e: Exception) {
                    // Network hiccup — retry
                    kotlinx.coroutines.delay(3000)
                    continue
                }

                when (status.status) {
                    "failed" -> {
                        job.phase = Phase.FAILED
                        job.error = status.error ?: "Download failed on server"
                        emit(job)
                        return@flow
                    }
                    "ready" -> {
                        // Server file is ready — go to phase 3
                        break
                    }
                    "merging" -> {
                        job.phase = Phase.MERGING
                        job.progress = status.progress
                        job.downloadedBytes = status.downloaded
                        job.speedBps = status.speedBps
                        emit(job)
                    }
                    else -> {  // "downloading"
                        job.phase = Phase.FETCHING
                        job.progress = status.progress
                        job.downloadedBytes = status.downloaded
                        job.speedBps = status.speedBps
                        emit(job)
                    }
                }
            }

            // PHASE 3: Download from server to phone in chunks
            job.phase = Phase.TRANSFERRING
            job.progress = 0
            job.downloadedBytes = 0L
            job.speedBps = 0L
            emit(job)

            val outputFile = File(outputDir, "${sanitizeFileName(title)}.$ext")
            val baseUrl = ApiClient.baseUrl.trimEnd('/')
            val chunkUrl = "$baseUrl/api/preload?id=${startRes.jobId}&title=${title}&ext=$ext"

            FileOutputStream(outputFile).use { fos ->
                var start = 0L
                val total = startRes.total
                val chunkSize = 30L * 1024 * 1024  // 30MB per request — fits in 30s gateway
                var lastSpeedAt = System.currentTimeMillis()
                var lastSpeedBytes = 0L

                while (start < total) {
                    val end = minOf(start + chunkSize - 1, total - 1)
                    val req = Request.Builder()
                        .url(chunkUrl)
                        .header("Range", "bytes=$start-$end")
                        .build()

                    httpClient.newCall(req).execute().use { res ->
                        if (!res.isSuccessful && res.code != 206) {
                            throw RuntimeException("HTTP ${res.code}")
                        }
                        val body = res.body ?: throw RuntimeException("No body")
                        body.byteStream().use { input ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                fos.write(buf, 0, n)
                                job.downloadedBytes += n
                                val now = System.currentTimeMillis()
                                val dt = (now - lastSpeedAt) / 1000.0
                                if (dt >= 1.0) {
                                    val instant = (job.downloadedBytes - lastSpeedBytes) / dt
                                    job.speedBps = instant.toLong()
                                    lastSpeedAt = now
                                    lastSpeedBytes = job.downloadedBytes
                                }
                                job.progress = ((job.downloadedBytes * 100) / total).toInt()
                                emit(job)
                            }
                        }
                    }
                    start = end + 1
                }
            }

            job.phase = Phase.COMPLETED
            job.progress = 100
            job.speedBps = 0L
            job.savedFilePath = outputFile.absolutePath
            emit(job)
        } catch (e: Exception) {
            job.phase = Phase.FAILED
            job.error = e.message ?: "Unknown error"
            emit(job)
        }
    }.flowOn(Dispatchers.IO)

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[<>:\"/\\\\|?*\\x00-\\x1f]"), "_")
            .take(100)
            .ifBlank { "video" }
    }
}
