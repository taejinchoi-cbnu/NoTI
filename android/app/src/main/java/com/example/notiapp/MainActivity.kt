package com.example.notiapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var recordButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingsAdapter: RecordingsAdapter

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: String? = null

    private val RECORD_AUDIO_PERMISSION_CODE = 200
    private val recordings = mutableListOf<RecordingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.BtnRecord)
        statusTextView = findViewById(R.id.recordingStatusText)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)

        // RecyclerView 설정
        recordingsAdapter = RecordingsAdapter(recordings) { recording ->
            deleteRecording(recording)
        }

        recordingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recordingsAdapter
        }

        // 녹음 파일 목록 로드
        loadRecordings()

        // 버튼 클릭 리스너 설정
        recordButton.setOnClickListener {
            if (checkPermission()) {
                if (!isRecording) {
                    startRecording()
                } else {
                    stopRecording()
                    // 녹음이 완료되면 녹음 파일 목록 갱신
                    loadRecordings()
                }
            } else {
                requestPermission()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "녹음 권한이 허용되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        try {
            // 녹음 파일 경로 설정
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioDir = File(getExternalFilesDir(null), "AudioRecords")

            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            outputFile = "${audioDir.absolutePath}/REC_$timeStamp.mp3"

            Log.d(TAG, "녹음 파일 경로: $outputFile")

            // MediaRecorder 설정
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)

                try {
                    prepare()
                    start()
                    isRecording = true

                    // UI 업데이트
                    recordButton.text = "녹음 중지"
                    statusTextView.text = "녹음 중..."

                    Log.d(TAG, "녹음 시작됨")
                    Toast.makeText(this@MainActivity, "녹음이 시작되었습니다", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Log.e(TAG, "녹음 준비 실패: ${e.message}")
                    Toast.makeText(this@MainActivity, "녹음 준비에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 시작 중 오류 발생: ${e.message}")
            Toast.makeText(this, "녹음 시작 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }

            mediaRecorder = null
            isRecording = false

            // UI 업데이트
            recordButton.text = "녹음"
            statusTextView.text = "녹음 완료: ${outputFile?.substringAfterLast('/')}"

            Log.d(TAG, "녹음 중지됨: $outputFile")
            Toast.makeText(this, "녹음이 저장되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "녹음 중지 중 오류 발생: ${e.message}")
            Toast.makeText(this, "녹음 중지 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()

            // 오류 발생 시 UI 초기화
            recordButton.text = "녹음"
            statusTextView.text = "녹음 대기 중"
            isRecording = false
        }
    }

    private fun loadRecordings() {
        val audioDir = File(getExternalFilesDir(null), "AudioRecords")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
            return
        }

        // 녹음 파일 목록 가져오기
        val recordingFiles = audioDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") }

        if (recordingFiles.isNullOrEmpty()) {
            Log.d(TAG, "녹음 파일이 없습니다")
            recordings.clear()
            recordingsAdapter.notifyDataSetChanged()
            return
        }

        // 날짜 내림차순으로 정렬 (최신 파일 먼저)
        val sortedFiles = recordingFiles.sortedByDescending { it.lastModified() }

        // 녹음 파일 정보 가져오기
        val newRecordings = sortedFiles.map { file ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateString = dateFormat.format(Date(file.lastModified()))

            // 파일 재생 시간 가져오기
            val duration = getAudioFileDuration(file)

            RecordingItem(
                file = file,
                filename = file.name,
                date = dateString,
                duration = duration,
                filePath = file.absolutePath
            )
        }

        // 어댑터 업데이트
        recordings.clear()
        recordings.addAll(newRecordings)
        recordingsAdapter.notifyDataSetChanged()

        Log.d(TAG, "녹음 파일 ${recordings.size}개 로드됨")
    }

    private fun getAudioFileDuration(file: File): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0

            retriever.release()

            formatDuration(durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "파일 길이 확인 중 오류: ${e.message}")
            "00:00"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                TimeUnit.MINUTES.toSeconds(minutes)

        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun deleteRecording(recording: RecordingItem) {
        try {
            if (recording.file.exists() && recording.file.delete()) {
                Log.d(TAG, "녹음 파일 삭제됨: ${recording.filename}")
                Toast.makeText(this, "녹음 파일이 삭제되었습니다", Toast.LENGTH_SHORT).show()

                // 목록에서 제거
                recordings.remove(recording)
                recordingsAdapter.notifyDataSetChanged()
            } else {
                Log.e(TAG, "녹음 파일 삭제 실패: ${recording.filename}")
                Toast.makeText(this, "녹음 파일 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 파일 삭제 중 오류: ${e.message}")
            Toast.makeText(this, "녹음 파일 삭제 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingsAdapter.releaseMediaPlayer()
    }
}