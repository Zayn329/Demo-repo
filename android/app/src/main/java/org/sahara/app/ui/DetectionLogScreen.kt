package org.sahara.app.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.sahara.core.domain.models.DetectorType
import org.sahara.services.detection.log.DetectionLogEvent
import org.sahara.services.detection.log.DetectionLogManager

@Composable
fun DetectionLogScreen(
    onBack: () -> Unit
) {
    val events by DetectionLogManager.events.collectAsState()
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var currentAudioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SaharaColors.WarmWhite)
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaharaColors.SurfaceSubtle)
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "← Back",
                    color = SaharaColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "Detection Log",
                color = SaharaColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            if (events.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SaharaColors.SurfaceSubtle)
                        .clickable { DetectionLogManager.clearLogs() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Clear",
                        color = SaharaColors.DangerCoral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(50.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Real-time log of unsealed signal detection events (screams, keywords, motion anomalies).",
            color = SaharaColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Detection Events Yet",
                        color = SaharaColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active safety monitoring will log screams, keywords, and motion signals here in real-time.",
                        color = SaharaColors.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { logItem ->
                    DetectionLogCard(
                        event = logItem,
                        isPlaying = currentlyPlayingId == logItem.id,
                        onPlayClick = {
                            if (currentlyPlayingId == logItem.id) {
                                try {
                                    currentAudioTrack?.stop()
                                    currentAudioTrack?.release()
                                } catch (_: Throwable) {}
                                currentAudioTrack = null
                                currentlyPlayingId = null
                            } else {
                                try {
                                    currentAudioTrack?.stop()
                                    currentAudioTrack?.release()
                                } catch (_: Throwable) {}
                                currentlyPlayingId = logItem.id
                                val audioData = logItem.audioData
                                if (audioData != null && audioData.isNotEmpty()) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val bufferSize = AudioTrack.getMinBufferSize(
                                                16000,
                                                AudioFormat.CHANNEL_OUT_MONO,
                                                AudioFormat.ENCODING_PCM_16BIT
                                            )
                                            val track = AudioTrack.Builder()
                                                .setAudioAttributes(
                                                    AudioAttributes.Builder()
                                                        .setUsage(AudioAttributes.USAGE_MEDIA)
                                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                        .build()
                                                )
                                                .setAudioFormat(
                                                    AudioFormat.Builder()
                                                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                                        .setSampleRate(16000)
                                                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                                        .build()
                                                )
                                                .setBufferSizeInBytes(Math.max(bufferSize, audioData.size * 2))
                                                .setTransferMode(AudioTrack.MODE_STATIC)
                                                .build()

                                            currentAudioTrack = track
                                            track.write(audioData, 0, audioData.size)
                                            track.play()

                                            val durationMs = (audioData.size * 1000L) / 16000L
                                            kotlinx.coroutines.delay(durationMs + 200L)
                                            track.stop()
                                            track.release()
                                        } catch (_: Throwable) {}
                                        currentlyPlayingId = null
                                    }
                                } else {
                                    currentlyPlayingId = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DetectionLogCard(
    event: DetectionLogEvent,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val detectorColor = when (event.signal.detectorType) {
        DetectorType.SCREAM -> Color(0xFFE53935)   // Red
        DetectorType.KEYWORD -> Color(0xFFFFB300)  // Amber
        DetectorType.MOTION -> Color(0xFF1E88E5)   // Blue
        else -> Color.Gray
    }
    val audioData = event.audioData

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SaharaColors.PureWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(detectorColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.signal.detectorType.name,
                            color = detectorColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = event.timestampFormatted,
                        color = SaharaColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Label: ",
                        color = SaharaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = event.signal.label ?: "detected",
                        color = SaharaColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Conf: ",
                        color = SaharaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "%.0f%%".format(event.signal.confidence * 100),
                        color = SaharaColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (audioData != null && audioData.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isPlaying) SaharaColors.DangerCoral else detectorColor)
                        .clickable { onPlayClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isPlaying) "Stop" else "▶ Play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
