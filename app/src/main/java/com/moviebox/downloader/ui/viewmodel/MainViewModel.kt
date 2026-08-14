package com.moviebox.downloader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebox.downloader.download.DownloadJob
import com.moviebox.downloader.download.DownloadManager
import com.moviebox.downloader.download.Phase
import com.moviebox.downloader.network.ApiClient
import com.moviebox.downloader.network.ExtractRequest
import com.moviebox.downloader.network.ExtractResponse
import com.moviebox.downloader.network.VideoFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val downloadManager = DownloadManager()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadJob>>(emptyList())
    val downloads: StateFlow<List<DownloadJob>> = _downloads.asStateFlow()

    private val _playingFormat = MutableStateFlow<VideoFormat?>(null)
    val playingFormat: StateFlow<VideoFormat?> = _playingFormat.asStateFlow()

    private val _selectedFormatId = MutableStateFlow<String?>(null)
    val selectedFormatId: StateFlow<String?> = _selectedFormatId.asStateFlow()

    fun selectFormat(formatId: String) {
        _selectedFormatId.value = formatId
    }

    fun playFormat(fmt: VideoFormat) {
        _playingFormat.value = fmt
    }

    fun extract(url: String) {
        if (url.isBlank()) return
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val res = ApiClient.api.extract(ExtractRequest(url))
                _uiState.value = UiState.Success(res)
                _selectedFormatId.value = res.formats.firstOrNull()?.let { "${it.formatId}-${it.ext}" }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to extract video")
            }
        }
    }

    fun startDownload(result: ExtractResponse, fmt: VideoFormat, outputDir: java.io.File) {
        viewModelScope.launch {
            try {
                downloadManager.start(
                    title = result.title,
                    ext = fmt.ext,
                    webpageUrl = result.webpageUrl,
                    format = fmt,
                    outputDir = outputDir,
                ).collectLatest { job ->
                    _downloads.value = _downloads.value.toMutableList().apply {
                        val idx = indexOfFirst { it.id == job.id }
                        if (idx >= 0) set(idx, job) else add(job)
                    }
                }
            } catch (_: Exception) {
                // handled inside DownloadManager (FAILED phase)
            }
        }
    }

    fun clearFinishedDownloads() {
        _downloads.value = _downloads.value.filter {
            it.phase != Phase.COMPLETED && it.phase != Phase.FAILED
        }
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val result: ExtractResponse) : UiState()
    data class Error(val message: String) : UiState()
}
