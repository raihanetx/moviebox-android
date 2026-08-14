package com.moviebox.downloader.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moviebox.downloader.download.DownloadJob
import com.moviebox.downloader.download.Phase
import com.moviebox.downloader.network.ExtractResponse
import com.moviebox.downloader.network.VideoFormat
import com.moviebox.downloader.ui.viewmodel.MainViewModel
import com.moviebox.downloader.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtractScreen(
    vm: MainViewModel,
    initialUrl: String?,
    onPlayVideo: () -> Unit,
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(initialUrl ?: "") }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val selectedFormatId by vm.selectedFormatId.collectAsStateWithLifecycle()
    val playingFormat by vm.playingFormat.collectAsStateWithLifecycle()

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("MovieBox", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(" Downloader", color = Color(0xFF71717A), fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                ),
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // URL input
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Paste video URL") },
                placeholder = { Text("https://themoviebox.xyz/...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = {
                    if (urlText.isNotEmpty()) {
                        IconButton(onClick = { urlText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { urlText = it }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0A0A0A),
                    unfocusedContainerColor = Color(0xFF0A0A0A),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF27272A),
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color(0xFF71717A),
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = { vm.extract(urlText) },
                enabled = urlText.isNotBlank() && uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Extract", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            when (val state = uiState) {
                is UiState.Success -> ResultCard(
                    result = state.result,
                    vm = vm,
                    selectedFormatId = selectedFormatId,
                    onPlayVideo = onPlayVideo,
                    onDownload = { fmt ->
                        // Save to public Downloads folder
                        val dir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        )
                        vm.startDownload(state.result, fmt, dir)
                    },
                )
                is UiState.Error -> ErrorCard(state.message)
                else -> Unit
            }

            // Downloads panel (sticky bottom)
            if (downloads.isNotEmpty()) {
                DownloadsCard(downloads = downloads, onClear = vm::clearFinishedDownloads)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(
    result: ExtractResponse,
    vm: MainViewModel,
    selectedFormatId: String?,
    onPlayVideo: () -> Unit,
    onDownload: (VideoFormat) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0A0A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = result.title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
        )

        // Metadata row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (result.isSeries && result.currentSeason != null && result.currentEpisode != null) {
                Chip("S${result.currentSeason}E${result.currentEpisode}")
            }
            result.duration?.let { Chip(it) }
            result.formats.firstOrNull()?.let { fmt ->
                if (fmt.width != null && fmt.height != null) {
                    Chip("${fmt.width}x${fmt.height}")
                }
            }
        }

        // Quality pills
        Text(
            "QUALITY",
            color = Color(0xFF71717A),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            result.formats.forEach { fmt ->
                val key = "${fmt.formatId}-${fmt.ext}"
                val isSelected = selectedFormatId == key
                FilterChip(
                    selected = isSelected,
                    onClick = { vm.selectFormat(key) },
                    label = {
                        Text(
                            text = formatLabel(fmt),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF18181B),
                        labelColor = Color.White,
                    ),
                )
            }
        }

        // Download + Play buttons
        val selectedFmt = result.formats.find { "${it.formatId}-${it.ext}" == selectedFormatId }
        if (selectedFmt != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onDownload(selectedFmt) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        vm.playFormat(selectedFmt)
                        onPlayVideo()
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3F46)),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Watch")
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF27272A))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Couldn't extract", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = Color(0xFFA1A1AA), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DownloadsCard(
    downloads: List<DownloadJob>,
    onClear: () -> Unit,
) {
    val active = downloads.count { it.phase != Phase.COMPLETED && it.phase != Phase.FAILED }
    val completed = downloads.count { it.phase == Phase.COMPLETED }
    val failed = downloads.count { it.phase == Phase.FAILED }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (active > 0) "$active downloading" else "Downloads",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                if (completed > 0 || failed > 0) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = Color(0xFF71717A), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            downloads.forEach { job ->
                DownloadRow(job)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DownloadRow(job: DownloadJob) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = job.title,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { job.progress / 100f },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = when (job.phase) {
                Phase.COMPLETED -> Color(0xFF22C55E)
                Phase.FAILED -> Color(0xFFEF4444)
                Phase.TRANSFERRING -> Color(0xFF3B82F6)
                else -> Color.White
            },
            trackColor = Color(0xFF27272A),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when (job.phase) {
                    Phase.QUEUED -> "Queued"
                    Phase.FETCHING -> "Fetching ${job.progress}%"
                    Phase.MERGING -> "Merging…"
                    Phase.TRANSFERRING -> "Saving to device ${job.progress}%"
                    Phase.COMPLETED -> "Saved"
                    Phase.FAILED -> "Failed: ${job.error ?: ""}"
                },
                color = Color(0xFFA1A1AA),
                fontSize = 10.sp,
            )
            if (job.speedBps > 0 && (job.phase == Phase.FETCHING || job.phase == Phase.TRANSFERRING)) {
                Text(
                    text = formatSpeed(job.speedBps),
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

private fun formatLabel(fmt: VideoFormat): String {
    val h = fmt.height
    return if (h != null && h > 0) "${h}p ${fmt.ext.uppercase()}" else fmt.ext.uppercase()
}

private fun formatSpeed(bps: Long): String {
    val mbps = bps / 1024.0 / 1024.0
    val kbps = bps / 1024.0
    return when {
        mbps >= 1.0 -> "%.1f MB/s".format(mbps)
        kbps >= 1.0 -> "%.0f KB/s".format(kbps)
        else -> "$bps B/s"
    }
}
