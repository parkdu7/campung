package com.shinhan.campung.presentation.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator   // ✅ 추가
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface                  // ✅ 추가
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shinhan.campung.R
import java.io.File
// 🔻 잘못된 임포트 제거: import java.nio.file.Files.delete

// ✅ 로컬 enum → 파일 최상단으로 이동
private enum class RecState { Idle, Recording, Stopped }

@Composable
fun RecordUploadDialog(
    isUploading: Boolean,
    onRequestAudioPermission: () -> Unit,
    onCancel: () -> Unit,
    onRegister: (File) -> Unit
) {
    val context = LocalContext.current

    var recState by remember { mutableStateOf(RecState.Idle) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    // 다이얼로그 닫힐 때 정리
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.apply {
                    runCatching { stop() }
                    reset()
                    release()
                }
            } catch (_: Exception) { }
            mediaRecorder = null
            outputFile = null
        }
    }

    fun startRecording() {
        try {
            // 권한 체크
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                onRequestAudioPermission()
                return
            }

            val file = File(context.cacheDir, "record_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                android.media.MediaRecorder()
            }

            recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            recState = RecState.Recording
        } catch (e: Exception) {
            recState = RecState.Idle
            outputFile = null
            mediaRecorder?.run {
                runCatching { stop() }
                reset()
                release()
            }
            mediaRecorder = null
            android.widget.Toast
                .makeText(context, "녹음 시작 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                runCatching { stop() }
                reset()
                release()
            }
        } catch (_: Exception) { /* stop 중 예외 무시 */ }
        mediaRecorder = null
        // 파일은 유지
        recState = RecState.Stopped
    }

    fun resetRecording() {
        try {
            mediaRecorder?.apply {
                runCatching { stop() }
                reset()
                release()
            }
        } catch (_: Exception) { }
        mediaRecorder = null
        // ✅ File.delete()만 사용
        outputFile?.delete()
        outputFile = null
        recState = RecState.Idle
    }

    val mainIconRes = when (recState) {
        RecState.Idle -> R.drawable.btn_recordstart
        RecState.Recording -> R.drawable.btn_recordstop
        RecState.Stopped -> R.drawable.btn_recordreset
    }

    val mainAction: () -> Unit = when (recState) {
        RecState.Idle -> ::startRecording
        RecState.Recording -> ::stopRecording
        RecState.Stopped -> ::resetRecording
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!isUploading) onCancel() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp, // 그림자 효과
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(min = 300.dp, max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "녹음을 등록해주세요!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF111827)
                )

                Spacer(Modifier.height(16.dp))

                // 중앙 큰 버튼 (시작/중단/리셋)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clickable(
                            enabled = !isUploading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { mainAction() }
                ) {
                    Image(
                        painter = painterResource(mainIconRes),
                        contentDescription = "record_button",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 상태 텍스트
                val status = when (recState) {
                    RecState.Idle -> "대기 중"
                    RecState.Recording -> "녹음 중..."
                    RecState.Stopped -> "녹음 완료"
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )

                Spacer(Modifier.height(20.dp))

                // 하단 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 취소 (E5E7EB)
                    Button(
                        onClick = {
                            if (recState == RecState.Recording) stopRecording()
                            resetRecording()
                            onCancel()
                        },
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE5E7EB),
                            contentColor = Color(0xFF111827)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("취소")
                    }

                    // 등록 (788CF7)
                    Button(
                        onClick = {
                            val file = outputFile
                            if (recState == RecState.Recording) {
                                // 녹음 중이면 먼저 정지
                                stopRecording()
                            }
                            if (file != null && recState == RecState.Stopped) {
                                onRegister(file)
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "녹음을 완료한 뒤 등록해주세요.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF788CF7),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("등록 중...")
                        } else {
                            Text("등록")
                        }
                    }
                }
            }
        }
    }
}
