package com.example.notiapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
// Markwon 라이브러리 import
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin

class RecordingDetailActivity : AppCompatActivity() {

    private val TAG = "RecordingDetailActivity"
    val serverIp = AddressAdmin.MY_SERVER_IP

    // UI 요소 변수 선언
    private lateinit var fileNameText: TextView
    private lateinit var fileInfoText: TextView

    // 탭 버튼들
    private lateinit var scriptTabButton: Button
    private lateinit var summaryTabButton: Button

    // 컨텐츠 레이아웃
    private lateinit var scriptContentLayout: LinearLayout
    private lateinit var summaryContentLayout: LinearLayout

    // 스크립트 관련
    private lateinit var scriptLoadingProgress: ProgressBar
    private lateinit var scriptScrollView: ScrollView
    private lateinit var scriptTextView: TextView
    private lateinit var copyScriptButton: ImageButton

    // 요약본 관련
    private lateinit var summaryLoadingProgress: ProgressBar
    private lateinit var summaryScrollView: ScrollView
    private lateinit var summaryTextView: TextView
    private lateinit var copySummaryButton: ImageButton

    // 오디오 플레이어 관련
    private lateinit var audioSeekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var rewind15Button: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var forward15Button: ImageButton

    private lateinit var bottomNavigationView: BottomNavigationView

    // 파일 정보 변수
    private var filePath: String = ""
    private var fileName: String = ""
    private var savedFileName: String = ""
    private var recordingDate: String = ""
    private var duration: String = ""
    private var isServerFile: Boolean = false
    private var isDownloaded: Boolean = false
    private var fileSize: Long = 0L
    private var uploadDate: String = ""

    // 미디어 플레이어
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    // 시간 업데이트 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateSeekBarRunnable: Runnable

    // HTTP 클라이언트
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 데이터 저장
    private var scriptData: String = ""
    private var summaryData: String = ""

    // Markwon 인스턴스 (마크다운 렌더링용)
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_recording_detail)

            // Edge-to-Edge 설정 유지
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recordingDetail)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Markwon 초기화 (마크다운 렌더링 라이브러리)
            initializeMarkwon()

            // UI 요소 초기화
            initializeViews()

            // Intent에서 파일 정보 추출
            extractFileInfoFromIntent()

            // 파일 정보 표시
            displayFileInfo()

            // 로컬 파일 정보 가져오기
            fetchSavedFileName()

            // 버튼 클릭 리스너 설정
            setupButtonListeners()

            // 하단 네비게이션 설정
            setupBottomNavigation()

            // 오디오 플레이어 설정
            setupAudioPlayer()

            // 화면 진입 시 자동으로 스크립트 로딩
            loadScriptData()

            Log.d(TAG, "onCreate 완료: fileName=$fileName, filePath=$filePath, savedFileName=$savedFileName")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 에러: ${e.message}", e)
            Toast.makeText(this, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * Markwon 라이브러리 초기화
     * 마크다운 텍스트를 Android TextView에서 렌더링할 수 있도록 설정
     */
    private fun initializeMarkwon() {
        markwon = Markwon.builder(this)
            .usePlugin(LinkifyPlugin.create()) // 링크 자동 감지 플러그인
            .build()

        Log.d(TAG, "Markwon 라이브러리 초기화 완료")
    }

    private fun initializeViews() {
        // 파일 정보
        fileNameText = findViewById(R.id.fileNameText)
        fileInfoText = findViewById(R.id.fileInfoText)

        // 탭 버튼
        scriptTabButton = findViewById(R.id.scriptTabButton)
        summaryTabButton = findViewById(R.id.summaryTabButton)

        // 컨텐츠 레이아웃
        scriptContentLayout = findViewById(R.id.scriptContentLayout)
        summaryContentLayout = findViewById(R.id.summaryContentLayout)

        // 스크립트 관련
        scriptLoadingProgress = findViewById(R.id.scriptLoadingProgress)
        scriptScrollView = findViewById(R.id.scriptScrollView)
        scriptTextView = findViewById(R.id.scriptTextView)
        copyScriptButton = findViewById(R.id.copyScriptButton)

        // 요약본 관련
        summaryLoadingProgress = findViewById(R.id.summaryLoadingProgress)
        summaryScrollView = findViewById(R.id.summaryScrollView)
        summaryTextView = findViewById(R.id.summaryTextView)
        copySummaryButton = findViewById(R.id.copySummaryButton)

        // 오디오 플레이어
        audioSeekBar = findViewById(R.id.audioSeekBar)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)
        rewind15Button = findViewById(R.id.rewind15Button)
        playPauseButton = findViewById(R.id.playPauseButton)
        forward15Button = findViewById(R.id.forward15Button)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun extractFileInfoFromIntent() {
        intent?.let {
            filePath = it.getStringExtra("filePath") ?: ""
            fileName = it.getStringExtra("fileName") ?: ""
            savedFileName = it.getStringExtra("serverSavedFileName") ?: ""
            recordingDate = it.getStringExtra("recordingDate") ?: ""
            duration = it.getStringExtra("duration") ?: ""
            isServerFile = it.getBooleanExtra("isServerFile", false)
            isDownloaded = it.getBooleanExtra("isDownloaded", false)
            fileSize = it.getLongExtra("fileSize", 0L)
            uploadDate = it.getStringExtra("uploadDate") ?: ""

            Log.d(TAG, "파일 정보 추출: fileName=$fileName, savedFileName=$savedFileName, isServerFile=$isServerFile")
        }
    }

    private fun displayFileInfo() {
        // 파일명 표시
        fileNameText.text = fileName

        // 파일 정보 표시 (날짜, 길이)
        fileInfoText.text = "$recordingDate | $duration"

        Log.d(TAG, "파일 정보 표시 완료")
    }

    private fun fetchSavedFileName() {
        if (isServerFile && savedFileName.isNotEmpty()) {
            Log.d(TAG, "서버 파일: Intent에서 전달받은 서버 저장 파일명 사용: $savedFileName")
            return
        }

        if (!isServerFile) {
            val file = File(filePath)
            if (file.exists()) {
                val originalFileName = file.name
                val sharedPreferences = getSharedPreferences("recording_files", MODE_PRIVATE)
                val serverSavedFileName = sharedPreferences.getString(originalFileName, "")

                if (!serverSavedFileName.isNullOrEmpty()) {
                    savedFileName = serverSavedFileName
                    Log.d(TAG, "로컬 파일: 서버 저장 파일명 사용: $savedFileName")
                } else {
                    savedFileName = originalFileName
                    Log.d(TAG, "로컬 파일: 서버 저장 파일명을 찾을 수 없어 원본 파일명 사용: $savedFileName")
                }
            } else {
                savedFileName = fileName
                Log.d(TAG, "로컬 파일: 파일을 찾을 수 없어 전달받은 파일명 사용: $savedFileName")
            }
        }
    }

    private fun setupButtonListeners() {
        // 스크립트 탭 버튼
        scriptTabButton.setOnClickListener {
            showScriptTab()
        }

        // 요약본 탭 버튼
        summaryTabButton.setOnClickListener {
            showSummaryTab()
            // 요약본이 아직 로드되지 않았으면 로드
            if (summaryData.isEmpty()) {
                loadSummaryData()
            }
        }

        // 복사 버튼들
        copyScriptButton.setOnClickListener {
            copyToClipboard("스크립트", scriptData)
        }

        copySummaryButton.setOnClickListener {
            copyToClipboard("요약본", summaryData)
        }
    }

    private fun setupAudioPlayer() {
        // 재생/일시정지 버튼
        playPauseButton.setOnClickListener {
            if (isPlaying) {
                pausePlayback()
            } else {
                if (mediaPlayer == null) {
                    playRecording()
                } else {
                    resumePlayback()
                }
            }
        }

        // 15초 뒤로
        rewind15Button.setOnClickListener {
            seekRelative(-15000)
        }

        // 15초 앞으로
        forward15Button.setOnClickListener {
            seekRelative(15000)
        }

        // 시크바 리스너
        audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer?.seekTo(progress)
                    updateTimeText(currentTimeText, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 시크바 업데이트 Runnable
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (isPlaying) {
                        val currentPosition = it.currentPosition
                        audioSeekBar.progress = currentPosition
                        updateTimeText(currentTimeText, currentPosition)
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }
    }

    private fun showScriptTab() {
        // 탭 버튼 스타일 변경
        scriptTabButton.setBackgroundResource(R.drawable.filter_button_active)
        scriptTabButton.setTextColor(resources.getColor(R.color.white, null))
        summaryTabButton.setBackgroundResource(R.drawable.filter_button_inactive)
        summaryTabButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        // 컨텐츠 전환
        scriptContentLayout.visibility = View.VISIBLE
        summaryContentLayout.visibility = View.GONE
    }

    private fun showSummaryTab() {
        // 탭 버튼 스타일 변경
        summaryTabButton.setBackgroundResource(R.drawable.filter_button_active)
        summaryTabButton.setTextColor(resources.getColor(R.color.white, null))
        scriptTabButton.setBackgroundResource(R.drawable.filter_button_inactive)
        scriptTabButton.setTextColor(resources.getColor(R.color.textSecondary, null))

        // 컨텐츠 전환
        scriptContentLayout.visibility = View.GONE
        summaryContentLayout.visibility = View.VISIBLE
    }

    private fun loadScriptData() {
        if (savedFileName.isEmpty()) {
            showScriptError("파일명을 찾을 수 없습니다")
            return
        }

        // 로딩 표시
        scriptLoadingProgress.visibility = View.VISIBLE
        scriptScrollView.visibility = View.GONE

        val token = getJwtToken()
        if (token.isEmpty()) {
            showScriptError("로그인이 필요합니다.")
            return
        }

        thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("savedFileName", savedFileName)
                    .build()

                val request = Request.Builder()
                    .url("http://${serverIp}/ai/stt")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "STT 응답: ${response.code}")

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                scriptData = jsonResponse.getString("stt")
                                displayScript(scriptData)
                            } catch (e: Exception) {
                                Log.e(TAG, "JSON 파싱 오류: ${e.message}", e)
                                showScriptError("응답 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            showScriptError("스크립트 생성에 실패했습니다.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                runOnUiThread {
                    showScriptError("서버 연결에 실패했습니다.")
                }
            }
        }
    }

    private fun loadSummaryData() {
        if (savedFileName.isEmpty()) {
            showSummaryError("파일명을 찾을 수 없습니다")
            return
        }

        // 로딩 표시
        summaryLoadingProgress.visibility = View.VISIBLE
        summaryScrollView.visibility = View.GONE

        val token = getJwtToken()
        if (token.isEmpty()) {
            showSummaryError("로그인이 필요합니다.")
            return
        }

        thread {
            try {
                val requestBody = FormBody.Builder()
                    .add("savedFileName", savedFileName)
                    .build()

                val request = Request.Builder()
                    .url("http://${serverIp}/ai/gemini")
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "요약 응답: ${response.code}")

                    runOnUiThread {
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                summaryData = jsonResponse.getString("summation")
                                displaySummary(summaryData)
                            } catch (e: Exception) {
                                Log.e(TAG, "JSON 파싱 오류: ${e.message}", e)
                                showSummaryError("응답 처리 중 오류가 발생했습니다.")
                            }
                        } else {
                            showSummaryError("요약 생성에 실패했습니다.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                runOnUiThread {
                    showSummaryError("서버 연결에 실패했습니다.")
                }
            }
        }
    }

    private fun displayScript(script: String) {
        scriptLoadingProgress.visibility = View.GONE
        scriptScrollView.visibility = View.VISIBLE
        scriptTextView.text = script
    }

    /**
     * Markwon 라이브러리를 사용하여 마크다운 텍스트를 렌더링
     * @param summary 마크다운 형식의 요약 텍스트
     */
    private fun displaySummary(summary: String) {
        summaryLoadingProgress.visibility = View.GONE
        summaryScrollView.visibility = View.VISIBLE

        // Markwon 라이브러리로 마크다운을 실제 스타일이 적용된 텍스트로 렌더링
        markwon.setMarkdown(summaryTextView, summary)

        Log.d(TAG, "마크다운 요약본 렌더링 완료")
    }

    private fun showScriptError(message: String) {
        scriptLoadingProgress.visibility = View.GONE
        scriptScrollView.visibility = View.VISIBLE
        scriptTextView.text = "오류: $message"
    }

    private fun showSummaryError(message: String) {
        summaryLoadingProgress.visibility = View.GONE
        summaryScrollView.visibility = View.VISIBLE
        summaryTextView.text = "오류: $message"
    }

    private fun copyToClipboard(label: String, text: String) {
        if (text.isEmpty()) {
            Toast.makeText(this, "$label 내용이 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "$label 복사 완료", Toast.LENGTH_SHORT).show()
    }

    // 오디오 재생 관련 메서드들은 기존 코드 유지
    private fun playRecording() {
        // 기존 playRecording 코드 유지
        try {
            releaseMediaPlayer()

            Log.d(TAG, "재생 시작: $filePath")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    stopPlayback()
                }
                setOnPreparedListener {
                    // 총 시간 설정
                    val duration = it.duration
                    audioSeekBar.max = duration
                    updateTimeText(totalTimeText, duration)

                    // 재생 시작
                    it.start()
                    this@RecordingDetailActivity.isPlaying = true
                    updatePlayButtonIcon()

                    // 시크바 업데이트 시작
                    handler.post(updateSeekBarRunnable)
                }
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.e(TAG, "재생 중 오류 발생: ${e.message}", e)
            Toast.makeText(this, "재생 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayButtonIcon()
        handler.removeCallbacks(updateSeekBarRunnable)
        Log.d(TAG, "재생 일시정지")
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        isPlaying = true
        updatePlayButtonIcon()
        handler.post(updateSeekBarRunnable)
        Log.d(TAG, "재생 재개")
    }

    private fun stopPlayback() {
        releaseMediaPlayer()
        isPlaying = false
        updatePlayButtonIcon()
        handler.removeCallbacks(updateSeekBarRunnable)
        audioSeekBar.progress = 0
        updateTimeText(currentTimeText, 0)
        Log.d(TAG, "재생 중지")
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        Log.d(TAG, "미디어 플레이어 해제")
    }

    private fun updatePlayButtonIcon() {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun seekRelative(offsetMs: Int) {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition + offsetMs).coerceIn(0, it.duration)
            it.seekTo(newPosition)
            audioSeekBar.progress = newPosition
            updateTimeText(currentTimeText, newPosition)
        }
    }

    private fun updateTimeText(textView: TextView, timeMs: Int) {
        val totalSeconds = timeMs / 1000  // 먼저 전체 초를 계산
        val minutes = totalSeconds / 60    // 분 계산
        val seconds = totalSeconds % 60    // 남은 초 계산
        textView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun setupBottomNavigation() {
        // 기존 setupBottomNavigation 코드 유지
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    finish()
                    true
                }
                R.id.navigation_chatbot -> {
                    val intent = Intent(this, ChatbotActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_user -> {
                    val intent = Intent(this, UserInfoActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        Log.d(TAG, "하단 네비게이션 설정 완료")
    }

    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", "") ?: ""
        Log.d(TAG, "JWT 토큰: ${if (token.isNotEmpty()) "존재함" else "없음"}")
        return token
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pausePlayback()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable)
        releaseMediaPlayer()
        Log.d(TAG, "onDestroy: 리소스 해제")
    }
}