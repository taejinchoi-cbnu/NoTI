package com.example.notiapp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

class RecordingDetailActivity : AppCompatActivity() {

    private val TAG = "RecordingDetailActivity"

    // UI 요소 변수 선언
    private lateinit var fileNameText: TextView
    private lateinit var scriptButton: CardView
    private lateinit var summaryButton: CardView
    private lateinit var playButton: CardView
    private lateinit var downloadButton: CardView
    private lateinit var bottomNavigationView: BottomNavigationView

    // 파일 정보 변수
    private var filePath: String = ""
    private var fileName: String = ""
    private var savedFileName: String = "" // 서버에 저장된 실제 파일명
    private var recordingDate: String = ""
    private var duration: String = ""
    private var isServerFile: Boolean = false // 서버 파일 여부
    private var isDownloaded: Boolean = false // 다운로드 여부
    private var fileSize: Long = 0L // 파일 크기
    private var uploadDate: String = "" // 업로드 날짜

    // 미디어 플레이어
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    // HTTP 클라이언트
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

            // 네비게이션 바의 하단 패딩을 제한
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
                val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // 하단 패딩을 5dp로 제한
                val maxBottomPadding = resources.getDimensionPixelSize(R.dimen.max_bottom_padding) // 5dp
                val bottomPadding = minOf(systemInsets.bottom, maxBottomPadding)
                view.setPadding(0, 0, 0, bottomPadding)
                insets
            }

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

            Log.d(TAG, "onCreate 완료: fileName=$fileName, filePath=$filePath, savedFileName=$savedFileName, isServerFile=$isServerFile, isDownloaded=$isDownloaded")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 에러: ${e.message}", e)
            // 오류 대화상자 표시 또는 안전한 오류 처리
            Toast.makeText(this, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            finish() // 안전하게 액티비티 종료
        }
    }

    private fun initializeViews() {
        fileNameText = findViewById(R.id.fileNameText)
        scriptButton = findViewById(R.id.scriptButton)
        summaryButton = findViewById(R.id.summaryButton)
        playButton = findViewById(R.id.playButton)
        downloadButton = findViewById(R.id.downloadButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun extractFileInfoFromIntent() {
        // Intent에서 파일 정보 추출
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

            Log.d(TAG, "파일 정보 추출: fileName=$fileName, filePath=$filePath, savedFileName=$savedFileName, isServerFile=$isServerFile, isDownloaded=$isDownloaded")
        }
    }

    private fun displayFileInfo() {
        // 파일명과 추가 정보 표시
        if (isServerFile) {
            val fileSizeKB = fileSize / 1024
            val downloadStatus = if (isDownloaded) "다운로드됨" else "미다운로드"
            fileNameText.text = "$fileName\n(서버 파일 - ${fileSizeKB}KB - $downloadStatus)"
        } else {
            fileNameText.text = fileName
        }
        Log.d(TAG, "파일 정보 표시 완료")
    }

    // 로컬 파일 정보 가져오기로 수정
// fetchSavedFileName() 메서드만 수정
    private fun fetchSavedFileName() {
        // 서버 파일인 경우 Intent에서 전달받은 savedFileName을 그대로 사용
        if (isServerFile && savedFileName.isNotEmpty()) {
            Log.d(TAG, "서버 파일: Intent에서 전달받은 서버 저장 파일명 사용: $savedFileName")
            return
        }

        // 로컬 파일인 경우에만 기존 로직 수행
        if (!isServerFile) {
            // 파일 경로에서 파일 객체 생성
            val file = File(filePath)

            if (file.exists()) {
                // 원본 파일명
                val originalFileName = file.name

                // SharedPreferences에서 서버 저장 파일명 확인
                val sharedPreferences = getSharedPreferences("recording_files", MODE_PRIVATE)
                val serverSavedFileName = sharedPreferences.getString(originalFileName, "")

                if (!serverSavedFileName.isNullOrEmpty()) {
                    // 서버에 저장된 파일명(UUID 포함)이 있으면 사용
                    savedFileName = serverSavedFileName
                    Log.d(TAG, "로컬 파일: 서버 저장 파일명 사용: $savedFileName")
                } else {
                    // 캐시된 정보가 없으면 원본 파일명 사용
                    savedFileName = originalFileName
                    Log.d(TAG, "로컬 파일: 서버 저장 파일명을 찾을 수 없어 원본 파일명 사용: $savedFileName")
                }
            } else {
                // 파일이 존재하지 않으면 Intent에서 받은 fileName 사용
                savedFileName = fileName
                Log.d(TAG, "로컬 파일: 파일을 찾을 수 없어 전달받은 파일명 사용: $savedFileName")
            }
        } else if (savedFileName.isEmpty()) {
            // 서버 파일인데 savedFileName이 비어있는 경우 (예외 상황)
            savedFileName = fileName
            Log.w(TAG, "서버 파일이지만 savedFileName이 비어있어 fileName 사용: $savedFileName")
        }
    }

    private fun setupButtonListeners() {
        // 스크립트 생성 버튼 클릭 리스너
        scriptButton.setOnClickListener {
            if (savedFileName.isEmpty()) {
                Toast.makeText(this, "파일 정보를 가져오는 중입니다. 잠시 후 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            requestSTT(savedFileName)
        }

        // 요약본 생성 버튼 클릭 리스너
        summaryButton.setOnClickListener {
            if (savedFileName.isEmpty()) {
                Toast.makeText(this, "파일 정보를 가져오는 중입니다. 잠시 후 다시 시도하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            requestSummary(savedFileName)
        }

        // 다운로드 버튼 클릭 리스너
        downloadButton.setOnClickListener {
            if (isServerFile && !isDownloaded) {
                downloadServerFile()
            } else {
                Toast.makeText(this, "이미 다운로드되었거나 로컬 파일입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 녹음 파일 재생 버튼 클릭 리스너
        playButton.setOnClickListener {
            if (isServerFile && !isDownloaded) {
                // 서버 파일이고 다운로드되지 않은 경우
                Toast.makeText(this, "먼저 파일을 다운로드해주세요.", Toast.LENGTH_LONG).show()
            } else if (filePath.isEmpty()) {
                // 파일 경로가 없는 경우
                Toast.makeText(this, "재생할 수 있는 파일이 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 재생 가능한 경우
                if (isPlaying) {
                    pausePlayback()
                } else {
                    playRecording()
                }
            }
        }

        // 다운로드 버튼 표시/숨김 처리
        if (isServerFile && !isDownloaded) {
            downloadButton.visibility = android.view.View.VISIBLE
        } else {
            downloadButton.visibility = android.view.View.GONE
        }

        Log.d(TAG, "버튼 리스너 설정 완료")
    }

    private fun downloadServerFile() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("파일 다운로드 중...")
            setCancelable(false)
            show()
        }

        // JWT 토큰 가져오기
        val token = getJwtToken()
        if (token.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            try {
                val downloadClient = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                // 서버 파일 다운로드 API 호출 (url 구현X)
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/$savedFileName")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                downloadClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        if (responseBody != null) {
                            // 다운로드 디렉토리 생성
                            val downloadsDir = File(getExternalFilesDir(null), "downloads")
                            if (!downloadsDir.exists()) {
                                downloadsDir.mkdirs()
                            }

                            // 파일 저장
                            val downloadedFile = File(downloadsDir, savedFileName)
                            saveFileFromResponse(responseBody, downloadedFile)

                            runOnUiThread {
                                progressDialog.dismiss()
                                Toast.makeText(this@RecordingDetailActivity, "다운로드 완료!", Toast.LENGTH_SHORT).show()

                                // 다운로드 상태 업데이트
                                isDownloaded = true
                                filePath = downloadedFile.absolutePath

                                // UI 업데이트
                                displayFileInfo()
                                downloadButton.visibility = android.view.View.GONE
                            }
                        } else {
                            runOnUiThread {
                                progressDialog.dismiss()
                                Toast.makeText(this@RecordingDetailActivity, "다운로드 실패: 응답이 비어있습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            progressDialog.dismiss()
                            val errorMessage = when (response.code) {
                                401 -> "인증이 만료되었습니다. 다시 로그인해주세요."
                                404 -> "서버에서 파일을 찾을 수 없습니다."
                                else -> "다운로드 실패: ${response.code}"
                            }
                            Toast.makeText(this@RecordingDetailActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "다운로드 오류: ${e.message}", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@RecordingDetailActivity, "다운로드 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFileFromResponse(responseBody: okhttp3.ResponseBody, file: File) {
        try {
            responseBody.byteStream().use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "파일 저장 오류: ${e.message}", e)
            throw e
        }
    }

    private fun setupBottomNavigation() {
        // 하단 네비게이션 설정
        bottomNavigationView.selectedItemId = R.id.navigation_home

        // 네비게이션 아이템 클릭 리스너
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 대시보드 화면으로 이동
                    finish() // 현재 화면 종료하면 대시보드로 돌아감
                    true
                }
                R.id.navigation_chatbot -> {
                    // 나만의 비서 화면으로 이동
                    val intent = Intent(this, ChatbotActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_user -> {
                    // 내정보 화면으로 이동
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

    private fun requestSTT(savedFileName: String) {
        // 저장된 파일명이 비어있는지 확인
        if (savedFileName.isEmpty()) {
            Toast.makeText(this, "파일명을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        // 로딩 다이얼로그 표시
        val progressDialog = ProgressDialog(this).apply {
            setMessage("스크립트 생성 중...")
            setCancelable(false)
            show()
        }

        // JWT 토큰 가져오기 및 로깅
        val token = getJwtToken()
        Log.d(TAG, "사용 중인 JWT 토큰: ${if (token.isNotEmpty()) token.substring(0, minOf(10, token.length)) + "..." else "없음"}")

        if (token.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(this, "로그인이 필요합니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            return
        }

        // 백그라운드 스레드에서 네트워크 요청 수행
        thread {
            try {
                // POST 요청 본문 생성
                val requestBody = FormBody.Builder()
                    .add("savedFileName", savedFileName) // 서버에 저장된 실제 파일명 사용 (UUID 접두사 포함)
                    .build()

                // API 엔드포인트 URL
                val url = "http://10.0.2.2:8080/ai/stt"
                Log.d(TAG, "STT 요청 URL: $url")
                Log.d(TAG, "요청 파라미터: savedFileName=$savedFileName") // 파라미터 로깅 추가

                // POST 요청 생성
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)  // POST 메서드 사용
                    .header("Authorization", "Bearer $token")
                    .build()

                Log.d(TAG, "요청 메서드: ${request.method}")
                Log.d(TAG, "요청 헤더: ${request.headers}")

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "STT 응답 코드: ${response.code}")
                    Log.d(TAG, "STT 응답 본문: $responseBody")

                    runOnUiThread {
                        progressDialog.dismiss()

                        if (response.isSuccessful && responseBody != null) {
                            try {
                                // JSON 응답 파싱
                                val jsonResponse = JSONObject(responseBody)
                                val sttText = jsonResponse.getString("stt")

                                // 결과 다이얼로그 표시
                                showSTTResultDialog(sttText)

                            } catch (e: Exception) {
                                Log.e(TAG, "JSON 파싱 오류: ${e.message}", e)
                                Toast.makeText(
                                    this,
                                    "응답 처리 중 오류가 발생했습니다: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            // 오류 처리
                            val errorMessage = when (response.code) {
                                401 -> "인증이 만료되었습니다. 다시 로그인해주세요."
                                403 -> "접근 권한이 없습니다. 관리자에게 문의하세요."
                                404 -> "스크립트를 찾을 수 없습니다."
                                405 -> "요청 방식이 잘못되었습니다. GET 대신 POST를 사용해야 합니다."
                                500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
                                else -> "스크립트 생성 실패: ${response.code}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "서버 연결에 실패했습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun requestSummary(savedFileName: String) {
        // 저장된 파일명이 비어있는지 확인
        if (savedFileName.isEmpty()) {
            Toast.makeText(this, "파일명을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        // 로딩 다이얼로그 표시
        val progressDialog = ProgressDialog(this).apply {
            setMessage("요약본 생성 중...")
            setCancelable(false)
            show()
        }

        // JWT 토큰 가져오기 및 로깅
        val token = getJwtToken()
        Log.d(TAG, "사용 중인 JWT 토큰: ${if (token.isNotEmpty()) token.substring(0, minOf(10, token.length)) + "..." else "없음"}")

        if (token.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(this, "로그인이 필요합니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            return
        }

        // 백그라운드 스레드에서 네트워크 요청 수행
        thread {
            try {
                // POST 요청 본문 생성
                val requestBody = FormBody.Builder()
                    .add("savedFileName", savedFileName) // 서버에 저장된 실제 파일명 사용
                    .build()

                // API 엔드포인트 URL
                val url = "http://10.0.2.2:8080/ai/gemini"
                Log.d(TAG, "요약 요청 URL: $url")
                Log.d(TAG, "요청 파라미터: savedFileName=$savedFileName") // 파라미터 로깅 추가

                // POST 요청 생성
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)  // POST 메서드 사용
                    .header("Authorization", "Bearer $token")
                    .build()

                Log.d(TAG, "요청 메서드: ${request.method}")
                Log.d(TAG, "요청 메서드: ${request.method}")
                Log.d(TAG, "요청 헤더: ${request.headers}")

                // 요청 실행
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "요약 응답 코드: ${response.code}")
                    Log.d(TAG, "요약 응답 본문: $responseBody")

                    runOnUiThread {
                        progressDialog.dismiss()

                        if (response.isSuccessful && responseBody != null) {
                            try {
                                // JSON 응답 파싱
                                val jsonResponse = JSONObject(responseBody)
                                val summaryText = jsonResponse.getString("summation")

                                // 결과 다이얼로그 표시
                                showSummaryResultDialog(summaryText)

                            } catch (e: Exception) {
                                Log.e(TAG, "JSON 파싱 오류: ${e.message}", e)
                                Toast.makeText(
                                    this,
                                    "응답 처리 중 오류가 발생했습니다: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            // 오류 처리
                            val errorMessage = when (response.code) {
                                401 -> "인증이 만료되었습니다. 다시 로그인해주세요."
                                403 -> "접근 권한이 없습니다. 관리자에게 문의하세요."
                                404 -> "요약본을 찾을 수 없습니다."
                                405 -> "요청 방식이 잘못되었습니다. GET 대신 POST를 사용해야 합니다."
                                500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도하세요."
                                else -> "요약본 생성 실패: ${response.code}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "네트워크 오류: ${e.message}", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "서버 연결에 실패했습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showSTTResultDialog(sttText: String) {
        // 다이얼로그용 뷰 인플레이션
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_stt_result, null)

        // 결과 텍스트 설정
        val resultTextView = dialogView.findViewById<TextView>(R.id.sttResultText)
        resultTextView.text = sttText

        // 다이얼로그 생성
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 복사 버튼 리스너 설정
        dialogView.findViewById<Button>(R.id.copyButton).setOnClickListener {
            copyTextToClipboard(sttText)
            Toast.makeText(this, "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
        }

        // 다이얼로그 표시
        dialog.show()
    }

    // 요약 결과를 보여주는 다이얼로그
    private fun showSummaryResultDialog(summaryText: String) {
        // 다이얼로그용 뷰 인플레이션
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_stt_result, null)

        // 다이얼로그 제목 변경
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.text = "요약 결과"

        // 결과 텍스트 설정
        val resultTextView = dialogView.findViewById<TextView>(R.id.sttResultText)
        resultTextView.text = summaryText

        // 다이얼로그 생성
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 복사 버튼 리스너 설정
        dialogView.findViewById<Button>(R.id.copyButton).setOnClickListener {
            copyTextToClipboard(summaryText)
            Toast.makeText(this, "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
        }

        // 다이얼로그 표시
        dialog.show()
    }

    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("STT 결과", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    // JWT 토큰 가져오기
    private fun getJwtToken(): String {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", "") ?: ""
        Log.d(TAG, "JWT 토큰: ${if (token.isNotEmpty()) "존재함 (${token.length}자)" else "없음"}")
        return token
    }

    private fun playRecording() {
        try {
            // 이전 미디어 플레이어 해제
            releaseMediaPlayer()

            Log.d(TAG, "재생 시작: $filePath")

            // 새로운 미디어 플레이어 생성 및 재생
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    stopPlayback()
                }
                prepare()
                start()
            }

            isPlaying = true
            updatePlayButtonText("녹음 파일 일시정지")

        } catch (e: IOException) {
            Log.e(TAG, "재생 중 오류 발생: ${e.message}", e)
            Toast.makeText(
                this,
                "재생 중 오류가 발생했습니다: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayButtonText("녹음 파일 재생")
        Log.d(TAG, "재생 일시정지")
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        isPlaying = true
        updatePlayButtonText("녹음 파일 일시정지")
        Log.d(TAG, "재생 재개")
    }

    private fun stopPlayback() {
        releaseMediaPlayer()
        isPlaying = false
        updatePlayButtonText("녹음 파일 재생")
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

    private fun updatePlayButtonText(text: String) {
        // CardView 내부의 TextView 찾기 및 텍스트 변경
        val textView = (playButton.getChildAt(0) as? TextView)
        textView?.text = text
    }

    override fun onPause() {
        super.onPause()
        // 화면 떠날 때 재생 중지
        pausePlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 메모리 누수 방지
        releaseMediaPlayer()
        Log.d(TAG, "onDestroy: 리소스 해제")
    }
}